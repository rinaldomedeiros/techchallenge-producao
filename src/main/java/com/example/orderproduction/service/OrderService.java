package com.example.orderproduction.service;

import com.example.orderproduction.config.RabbitMQConfig;
import com.example.orderproduction.model.Order;
import com.example.orderproduction.model.OrderStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class OrderService {

    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);
    private static final String ORDER_KEY_PREFIX = "order:";

    private final RedisTemplate<String, Object> redisTemplate;
    private final RabbitTemplate rabbitTemplate;

    public OrderService(RedisTemplate<String, Object> redisTemplate, RabbitTemplate rabbitTemplate) {
        this.redisTemplate = redisTemplate;
        this.rabbitTemplate = rabbitTemplate;
    }

    public void processOrder(Order order) {
        if (order.getStatus() == null) {
            order.setStatus(OrderStatus.RECEIVED);
        }

        String key = ORDER_KEY_PREFIX + order.getOrderId();
        redisTemplate.opsForValue().set(key, order, 30, TimeUnit.MINUTES);
    }

    public Order updateOrderStatus(int orderId, OrderStatus newStatus) {

        String key = ORDER_KEY_PREFIX + orderId;
        Order order = (Order) redisTemplate.opsForValue().get(key);
        if (order == null) {
            throw new RuntimeException("Pedido não encontrado com id: " + orderId);
        }
        order.setStatus(newStatus);
        redisTemplate.opsForValue().set(key, order);

        Map<String, Object> updatedOrderMessage = new HashMap<>();
        updatedOrderMessage.put("orderId", order.getOrderId());
        updatedOrderMessage.put("orderStatus", order.getStatus());

        rabbitTemplate.convertAndSend(RabbitMQConfig.UPDATED_ORDER_EXCHANGE,
                RabbitMQConfig.UPDATED_ORDER_ROUTING_KEY, updatedOrderMessage);

        logger.info("Enviado o pedido {} para fila de pedidos atualizados com o status {}", order.getOrderId(), order.getStatus());
        return order;
    }


    public Order getOrder(int orderId) {
        String key = ORDER_KEY_PREFIX + orderId;
        return (Order) redisTemplate.opsForValue().get(key);
    }

    public List<Order> getOrdersByStatus(OrderStatus status) {
        Set<String> keys = redisTemplate.keys(ORDER_KEY_PREFIX + "*");
        List<Order> orders = new ArrayList<>();
        if (keys != null && !keys.isEmpty()) {
            for (String key : keys) {
                Order order = (Order) redisTemplate.opsForValue().get(key);
                if (order != null && order.getStatus() == status) {
                    orders.add(order);
                }
            }
        }
        return orders;
    }
}
