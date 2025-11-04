package com.hmall.trade.config;

import com.hmall.common.utils.UserContext;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aopalliance.aop.Advice;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 消息隊列配置
 */
@Slf4j
@Configuration
@AllArgsConstructor
@EnableConfigurationProperties(MandatoryProperties.class)
public class MqConfig {

    // ***********************傳遞user-id
    public static final String USER_ID_HEADER = "user-id";

    // 消息格式轉換器，轉化成統一格式
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    // 全局發送消息之前吧user-id放入Header
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory cf, MessageConverter mc, MandatoryProperties mandatoryProperties) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(cf);
        rabbitTemplate.setMessageConverter(mc);
        rabbitTemplate.setBeforePublishPostProcessors(message -> {
            Long userId = UserContext.getUser();
            if (userId != null) {
                message.getMessageProperties().setHeader(USER_ID_HEADER, userId);
            }
            return message;
        });
        // 設置returnCallBack
        rabbitTemplate.setReturnsCallback(returned -> log.error("觸發return callback, exchange={}, routingKey={}, replyText={}",
                returned.getExchange(), returned.getRoutingKey(), returned.getReplyText()));
        // 設置ConfirmCallback
        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            String id = correlationData == null ? null : correlationData.getId();
            if (ack) {
                log.info("[global-confirm] message {} acked by broker", id);
            } else {
                log.error("[global-confirm] message {} NOT acked, cause={}", id, cause);
                // 可以做通用警告，度量
            }
        });
        // 若希望 broker 返回无路由消息，确保 mandatory=true
        rabbitTemplate.setMandatory(mandatoryProperties.isMandatory());
        return rabbitTemplate;
    }

    //     接收隊列消息之前將userId放入到UserContext中
    @Bean
    public Advice userContextAdvice() {
        return (org.aopalliance.intercept.MethodInterceptor) invocation -> {
            try {
                // 同时兼容 Spring Messaging 同 AMQP Message
                for (Object arg : invocation.getArguments()) {
                    if (arg instanceof org.springframework.messaging.Message) {
                        Object uid = ((org.springframework.messaging.Message<?>) arg)
                                .getHeaders().get(USER_ID_HEADER);
                        if (uid != null) UserContext.setUser(Long.valueOf(uid.toString()));
                        break;
                    } else if (arg instanceof org.springframework.amqp.core.Message) {
                        Object uid = ((org.springframework.amqp.core.Message) arg)
                                .getMessageProperties().getHeaders().get(USER_ID_HEADER);
                        if (uid != null) UserContext.setUser(Long.valueOf(uid.toString()));
                        break;
                    }
                }
                return invocation.proceed();
            } finally {
                UserContext.removeUser(); // 非常重要：listener 线程会复用
            }
        };
    }

    @Bean(name = "rabbitListenerContainerFactory")
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory cf,
            MessageConverter mc,
            org.aopalliance.aop.Advice userContextAdvice) {

        SimpleRabbitListenerContainerFactory f = new SimpleRabbitListenerContainerFactory();
        f.setConnectionFactory(cf);
        f.setMessageConverter(mc);
        f.setAdviceChain(userContextAdvice);
        // 可选优化：按你业务负载调这些
        // f.setAcknowledgeMode(AcknowledgeMode.AUTO);
        // f.setPrefetchCount(50);
        return f;
    }


}