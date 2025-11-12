package com.hmall.api.config;

import com.hmall.api.client.fallback.*;
import com.hmall.common.utils.UserContext;
import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;

public class DefaultFeignConfig {
//    @Bean
//    public Logger.Level feignLogLevel() {
//        return Logger.Level.FULL;
//    }

    @Bean
    public RequestInterceptor userInfoRequestInterceptor() {
        return requestTemplate -> {
            Long userId = UserContext.getUser();
            if (userId == null) {
                return;
            }
            requestTemplate.header("user-info", userId.toString());
        };
    }

    @Bean
    public ItemClientFallback itemClientFallback() {
        return new ItemClientFallback();
    }

    @Bean
    public CartClientFallback cartClientFallback() {
        return new CartClientFallback();
    }

    @Bean
    public OrderClientFallback orderClientFallback() {
        return new OrderClientFallback();
    }

    @Bean
    public UserClientFallback userClientFallback() {
        return new UserClientFallback();
    }

    @Bean
    public PayClientFallback payClientFallback() {
        return new PayClientFallback();
    }
}