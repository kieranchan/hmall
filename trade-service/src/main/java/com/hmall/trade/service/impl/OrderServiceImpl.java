package com.hmall.trade.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmall.api.client.CartClient;
import com.hmall.api.client.ItemClient;
import com.hmall.api.domain.dto.ItemDTO;
import com.hmall.api.domain.dto.OrderDetailDTO;
import com.hmall.common.exception.BadRequestException;
import com.hmall.common.utils.UserContext;
import com.hmall.trade.constants.MQConstants;
import com.hmall.trade.domain.dto.OrderFormDTO;
import com.hmall.trade.domain.po.Order;
import com.hmall.trade.domain.po.OrderDetail;
import com.hmall.trade.mapper.OrderMapper;
import com.hmall.trade.service.IOrderDetailService;
import com.hmall.trade.service.IOrderService;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author
 * @since 2023-05-05
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements IOrderService {

    //    private final IItemService itemService;
    private final ItemClient itemClient;
    private final IOrderDetailService detailService;
    //    private final ICartService cartService;
    private final CartClient cartClient;
    private final RabbitTemplate rabbitTemplate;

    @Override
//    @Transactional
    @GlobalTransactional // 开启分布式事务，各个微服务的数据库都会进行回滚操作
    public Long createOrder(OrderFormDTO orderFormDTO) {
        // 1.订单数据
        Order order = new Order();
        // 1.1.查询商品
        List<OrderDetailDTO> detailDTOS = orderFormDTO.getDetails();
        // 1.2.获取商品id和数量的Map
        Map<Long, Integer> itemNumMap = detailDTOS.stream()
                .collect(Collectors.toMap(OrderDetailDTO::getItemId, OrderDetailDTO::getNum));
        Set<Long> itemIds = itemNumMap.keySet();
        // 1.3.查询商品
//        List<ItemDTO> items = itemService.queryItemByIds(itemIds);
        List<ItemDTO> items = itemClient.queryItemByIds(itemIds);
        if (items == null || items.size() < itemIds.size()) {
            throw new BadRequestException("商品不存在");
        }
        // 1.4.基于商品价格、购买数量计算商品总价：totalFee
        int total = 0;
        for (ItemDTO item : items) {
            total += item.getPrice() * itemNumMap.get(item.getId());
        }
        order.setTotalFee(total);
        // 1.5.其它属性
        order.setPaymentType(orderFormDTO.getPaymentType());
//        order.setUserId(1L);// 有弹幕说需要将这里改成1L
        order.setUserId(UserContext.getUser());// 有弹幕说需要将这里改成1L
        order.setStatus(1);
        // 1.6.将Order写入数据库order表中
        save(order);
        // 2.保存订单详情
        List<OrderDetail> details = buildDetails(order.getId(), items, itemNumMap);
        detailService.saveBatch(details);
        // 3.清理购物车商品
        // 進行異步改造
        try {
            String exchangeName = "trade.topic";
            String routingKey = "order.create";
//            rabbitTemplate.convertAndSend(exchangeName, routingKey, itemIds, message -> {
//                // 因爲mq的傳遞是不帶有userId的，所以需要自己傳遞，使用請求頭的方式傳遞
//                message.getMessageProperties().setHeader("userId", UserContext.getUser());
//                return message;
//            });
            rabbitTemplate.convertAndSend(exchangeName, routingKey, itemIds);
        } catch (Exception e) {
            log.error("清理購物車的消息發送失敗，購物車中的商品id為：{}", itemIds, e);
        }
        // 4.扣减库存
        try {
//            itemService.deductStock(detailDTOS);
            itemClient.deductStock(detailDTOS);
        } catch (Exception e) {
            throw new RuntimeException("库存不足！");
        }
        // 5.發送延遲消息，檢查訂單超時
        try {
            rabbitTemplate.convertAndSend(
                    MQConstants.DELAY_EXCHANGE_NAME,
                    MQConstants.DELAY_ROUTING_KEY,
                    order.getId(),
                    message -> {
                        message.getMessageProperties().setDelay(10000);// 设置10秒的超时时间
                        return message;
                    }
            );
        } catch (Exception e) {
            log.error("檢查訂單超時失敗，訂單id為：{}", order.getId());
        }
        return order.getId();
    }

    // 修改訂單狀態
    // 此處因爲需要進行異步操作，所以進行改造，需要在監聽器中調用
    @Override
    public void markOrderPaySuccess(Long orderId) {
//        Order order = new Order();
//        order.setId(orderId);
//        order.setStatus(2);
//        order.setPayTime(LocalDateTime.now());
//        updateById(order);
        // 通過業務判斷檢查冪等性，但是如下面這麽寫的話就有可能發生綫程問題
//        Order orderById = getById(orderId);
//        if (orderById == null || orderById.getStatus() != 1) {
//            return;
//        }
        // 實際的更新部分
        // UPDATE `order` SET status = ? , pay_time = ? WHERE id = ? AND status = 1
        lambdaUpdate()
                .set(Order::getPayTime, LocalDateTime.now())
                .set(Order::getStatus, 2)
                .eq(Order::getId, orderId)
                .eq(Order::getStatus, 1)// 重點在這一步，在更新時同時判斷status
                .update();
    }

    /**
     * 取消訂單，返回庫存
     *
     * @param orderId
     */
    @Override
    public void cancelOrder(Long orderId) {

    }

    private List<OrderDetail> buildDetails(Long orderId, List<ItemDTO> items, Map<Long, Integer> numMap) {
        List<OrderDetail> details = new ArrayList<>(items.size());
        for (ItemDTO item : items) {
            OrderDetail detail = new OrderDetail();
            detail.setName(item.getName());
            detail.setSpec(item.getSpec());
            detail.setPrice(item.getPrice());
            detail.setNum(numMap.get(item.getId()));
            detail.setItemId(item.getId());
            detail.setImage(item.getImage());
            detail.setOrderId(orderId);
            details.add(detail);
        }
        return details;
    }
}