//package com.hmall.gateway.mygatewayfilter;
//
//import lombok.Data;
//import org.springframework.cloud.gateway.filter.GatewayFilter;
//import org.springframework.cloud.gateway.filter.OrderedGatewayFilter;
//import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
//import org.springframework.stereotype.Component;
//
//import java.util.List;
//
//@Component
//public class PrintAnyGatewayFilterFactory extends AbstractGatewayFilterFactory<PrintAnyGatewayFilterFactory.Config> {
//
//    PrintAnyGatewayFilterFactory() {
//        super(Config.class);
//    }
//
//    @Override
//    public GatewayFilter apply(Config config) {
//        return new OrderedGatewayFilter((exchange, chain) -> chain.filter(exchange), 100);
////        return (exchange, chain) -> {
////            ServerHttpRequest request = exchange.getRequest();
////            System.out.println("過濾器執行了");
////            System.out.println(config);
////            return chain.filter(exchange);
////        };
//    }
//
//    @Data
//    static class Config {
//        private String a;
//        private String b;
//        private String c;
//    }
//
//    @Override
//    public List<String> shortcutFieldOrder() {
//        return List.of("a", "b", "c");
//    }
//
//    @Override
//    public Class<Config> getConfigClass() {
//        return Config.class;
//    }
//}