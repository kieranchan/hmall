package com.hmall.cart.listener;

import com.hmall.cart.service.ICartService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Collection;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClearItemsListener {
    private final ICartService cartService;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "cart.clear.queue"),
            exchange = @Exchange(name = "trade.topic", type = ExchangeTypes.TOPIC),
            key = "order.create"
    ))
    public void listenClearCart(Collection<Long> itemIds) {
//    public void listenClearCart(Collection<Long> itemIds, @Header("userId") Long userId) {
//        log.info("訂單號已經創建，正在清理id為{}用戶的購物車!", userId);
//        UserContext.setUser(userId);
        log.info("訂單號已經創建，正在清理id為{}用戶的購物車!", "userId");
        cartService.removeByItemIds(itemIds);
    }
}
