package com.sky.MQMessageReceiver;

import com.alibaba.fastjson.JSON;
import com.sky.config.RabbitMQConfiguration;
import com.sky.constant.DelayTimeConstant;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.websocket.WebSocketServer;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Component
public class MQMessageReceiver {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private WebSocketServer webSocketServer;

    @Autowired
    private OrderMapper orderMapper;

    // 接收支付成功通知
    @RabbitListener(queues = RabbitMQConfiguration.ORDER_PAY_SUCCESS_QUEUE)
    public void handlePaySuccess(String json) {
        webSocketServer.sendToAllClient(json);
    }

    // 延迟取消订单处理逻辑
    @RabbitListener(queues = RabbitMQConfiguration.ORDER_DEAD_QUEUE)
    public void cancelOrder(String json) {
        Map<String, Object> map = JSON.parseObject(json, Map.class);
        Long orderId = Long.valueOf(map.get("orderId").toString());
        Integer delayIndex = Integer.valueOf(map.get("delayIndex").toString());

        Orders order = orderMapper.getById(orderId);
        if (order == null) return;

        // 如果已支付（或状态不是“待支付”），说明已经完成，不处理
        if (!Orders.PENDING_PAYMENT.equals(order.getStatus())) {
            return;
        }

        // 如果还有下一次延迟机会，则继续发送下一轮延迟消息
        if (delayIndex + 1 < DelayTimeConstant.DELAY_TIMES.length) {
            Map<String, Object> newMsg = new HashMap<>();
            newMsg.put("orderId", orderId);
            newMsg.put("delayIndex", delayIndex + 1);

            rabbitTemplate.convertAndSend(
                    RabbitMQConfiguration.ORDER_CANCEL_EXCHANGE,
                    RabbitMQConfiguration.ORDER_DELAY_QUEUE,
                    JSON.toJSONString(newMsg),
                    message -> {
                        message.getMessageProperties().setExpiration(String.valueOf(DelayTimeConstant.DELAY_TIMES[delayIndex + 1]));
                        return message;
                    }
            );
        } else {
            // 最后一轮：再次确认数据库状态，避免误删
            Orders finalCheckOrder = orderMapper.getById(orderId);
            if (!Orders.PENDING_PAYMENT.equals(finalCheckOrder.getStatus())) {
                return; // 最后一轮仍已支付，则不取消
            }

            // 确认未支付，执行取消逻辑
            finalCheckOrder.setStatus(Orders.CANCELLED);
            finalCheckOrder.setCancelTime(LocalDateTime.now());
            finalCheckOrder.setCancelReason("超时未支付，系统自动取消");
            orderMapper.update(finalCheckOrder);

        }
    }

}
