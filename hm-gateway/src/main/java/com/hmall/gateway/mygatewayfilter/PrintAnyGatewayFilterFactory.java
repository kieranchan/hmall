package com.hmall.gateway.mygatewayfilter;

import lombok.Data;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class PrintAnyGatewayFilterFactory  extends
        AbstractGatewayFilterFactory<PrintAnyGatewayFilterFactory.Config> {
    
    PrintAnyGatewayFilterFactory(){
        super(Config.class);
    }
    
    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            System.out.println("過濾器執行了");
            
            return chain.filter(exchange);
        };
    }
    
    @Data
    static class Config{
        private String a;
        private String b;
        private String c;
    }

    @Override
    public List<String> shortcutFieldOrder() {
        return List.of("a","b","c");
    }

    @Override
    public Class<Config> getConfigClass() {
        return Config.class;
    }
}