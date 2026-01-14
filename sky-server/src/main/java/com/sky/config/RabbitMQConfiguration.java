package com.sky.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RabbitMQConfiguration {

    // 支付成功通知（普通队列）
    public static final String ORDER_PAY_SUCCESS_QUEUE = "order.pay.success";
    public static final String ORDER_PAY_EXCHANGE = "order.pay.direct";

    // 延迟取消订单（延迟队列 + 死信机制）
    public static final String ORDER_DELAY_QUEUE = "order.cancel.delay";
    public static final String ORDER_DEAD_QUEUE = "order.cancel.dead";
    public static final String ORDER_CANCEL_EXCHANGE = "order.cancel.direct";

    @Bean
    public DirectExchange orderPayExchange() {
        return new DirectExchange(ORDER_PAY_EXCHANGE);
    }

    @Bean
    public Queue orderPayQueue() {
        return new Queue(ORDER_PAY_SUCCESS_QUEUE);
    }

    @Bean
    public Binding payBinding() {
        return BindingBuilder.bind(orderPayQueue()).to(orderPayExchange()).with(ORDER_PAY_SUCCESS_QUEUE);
    }

    // 延迟队列（配置死信交换机）
    @Bean
    public Queue orderDelayQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", ORDER_CANCEL_EXCHANGE);
        args.put("x-dead-letter-routing-key", ORDER_DEAD_QUEUE);
        return new Queue(ORDER_DELAY_QUEUE, true, false, false, args);
    }

    // 死信队列
    @Bean
    public Queue orderDeadQueue() {
        return new Queue(ORDER_DEAD_QUEUE);
    }

    @Bean
    public DirectExchange orderCancelExchange() {
        return new DirectExchange(ORDER_CANCEL_EXCHANGE);
    }

    @Bean
    public Binding delayBinding() {
        return BindingBuilder.bind(orderDelayQueue()).to(orderCancelExchange()).with(ORDER_DELAY_QUEUE);
    }

    @Bean
    public Binding deadBinding() {
        return BindingBuilder.bind(orderDeadQueue()).to(orderCancelExchange()).with(ORDER_DEAD_QUEUE);
    }
}

