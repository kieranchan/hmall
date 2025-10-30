package com.hmall.api.client.fallback;

import com.hmall.api.client.CartClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;

@Slf4j
public class CartClientFallback implements FallbackFactory<CartClient> {
    @Override
    public CartClient create(Throwable cause) {
        return ids -> {
            // 清理购物车-失败了会怎么样子呢
            log.error("远程调用CartClient#deleteCartItemByIds方法出现异常，参数：{}", ids, cause);
        };
    }
}
