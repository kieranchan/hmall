//package com.hmall.gateway.mygatewayfilter;
//
//import org.springframework.cloud.gateway.filter.GatewayFilterChain;
//import org.springframework.cloud.gateway.filter.GlobalFilter;
//import org.springframework.core.Ordered;
//import org.springframework.stereotype.Component;
//import org.springframework.web.server.ServerWebExchange;
//import reactor.core.publisher.Mono;
//
//@Component
//public class PrintAnyGlobalFilter implements GlobalFilter, Ordered {
//
//    @Override
//    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
//        System.out.println("Pre阶段，请求处理前");
//        // 放行
//        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
//            // Post阶段
//            System.out.println("Post阶段，请求处理后");
//        }));
//        // 拦截
////        ServerHttpResponse response = exchange.getResponse();
////        response.setRawStatusCode(401);
////        return response.setComplete();
//    }
//
//    @Override
//    public int getOrder() {
//        return 0;
//    }
//}
