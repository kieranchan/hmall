package com.hmall.trade.listener;

import com.hmall.api.client.PayClient;
import com.hmall.api.domain.dto.PayOrderDTO;
import com.hmall.trade.constants.MQConstants;
import com.hmall.trade.domain.po.Order;
import com.hmall.trade.service.IOrderService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@AllArgsConstructor
public class OrderDelayMessageListener {
    private final IOrderService orderService;
    private final PayClient payClient;

    /**
     * 监听消息，查询支付状态
     * </br></br>
     * 1. 防止成功支付卻通知訂單失敗（通過mq通知的）</br>
     * 2. 超时订单的商品需要返回仓库</br></br>
     * 監聽事務：</br>
     * 1.1 监听订单是否已支付，支付過了就不處理，未支付就查询支付订单的状态</br>
     * 1.2 若支付订单状态为3，表示已支付，需要修改订单状态</br>
     * 1.3 若为其他，则做撤回操作</br>
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = MQConstants.DELAY_QUEUE_NAME),
            exchange = @Exchange(name = MQConstants.DELAY_EXCHANGE_NAME, delayed = "true"),
            key = MQConstants.DELAY_ROUTING_KEY
    ))
    public void listenOrderDelayMessage(Long orderId) {

        // 1. 获取OrderId
        Order order = orderService.getById(orderId);
        // 判断订单是否存在
        if (order == null || order.getStatus() != 1) {
            //  訂單不存在，或者已支付。則不操作
            return;
        }
        // 訂單未支付，需要查詢支付流水狀態
        // 2. 取得支付订单
        PayOrderDTO payOrderDTO = payClient.queryPayOrderByBizOrderNo(orderId);
        // 3. 是否支付
        if (payOrderDTO != null && payOrderDTO.getStatus() == 3) {// 非null且status = 3
            // 訂單不存在，還活著訂單已支付，則修改order表為已支付
            orderService.markOrderPaySuccess(orderId);
        }else {
            // 为null，或者未支付
            // 4. 取消訂單，返回庫存
            orderService.cancelOrder(orderId);

        }
    }
}
