package com.hmall.cart.config;

import com.hmall.common.utils.UserContext;
import org.aopalliance.aop.Advice;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 通過消息隊列無感傳遞user-id
 */
@Configuration
public class MqUserContextConfig {

    public static final String USER_ID_HEADER = "user-id";

    // 消息格式轉換器，轉化成統一格式
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    // 全局發送消息之前吧user-id放入Header
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory cf, MessageConverter mc) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(cf);
        rabbitTemplate.setMessageConverter(mc);
        rabbitTemplate.setBeforePublishPostProcessors(message -> {
            Long userId = UserContext.getUser();
            if (userId != null) {
                message.getMessageProperties().setHeader(USER_ID_HEADER, userId);
            }
            return message;
        });
        return rabbitTemplate;
    }

    // 接收隊列消息之前將userId放入到UserContext中
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
            Advice userContextAdvice) {

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