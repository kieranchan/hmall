package com.hmall.api.client.fallback;

import com.hmall.api.client.UserClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;

@Slf4j
public class UserClientFallback implements FallbackFactory<UserClient> {
    @Override
    public UserClient create(Throwable cause) {
        return (pw, amount) -> log.error("远程调用失败！",cause);
    }
}
