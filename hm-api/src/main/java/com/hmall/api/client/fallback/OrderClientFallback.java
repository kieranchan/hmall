package com.hmall.api.client.fallback;

import com.hmall.api.client.OrderClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;

@Slf4j
public class OrderClientFallback implements FallbackFactory<OrderClient> {
    @Override
    public OrderClient create(Throwable cause) {
        return orderId -> log.error("远程调用失败", cause);
    }
}
