package com.hmall.trade.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "spring.rabbitmq.template")
public class MandatoryProperties {
    private boolean mandatory;
}
