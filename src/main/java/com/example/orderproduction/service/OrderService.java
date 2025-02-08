package com.example.orderproduction.service;

import com.example.orderproduction.config.RabbitMQConfig;
import com.example.orderproduction.model.Order;
import com.example.orderproduction.model.OrderStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
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
            order.setStatus(OrderStatus.RECEBIDO);
        }

        redisTemplate.opsForValue().set(ORDER_KEY_PREFIX + order.getId(), order, 30, TimeUnit.MINUTES);
    }

    public Order updateOrderStatus(String orderId, OrderStatus newStatus) {
        String key = ORDER_KEY_PREFIX + orderId;
        Order order = (Order) redisTemplate.opsForValue().get(key);
        if (order == null) {
            throw new RuntimeException("Pedido não encontrado com id: " + orderId);
        }
        order.setStatus(newStatus);
        redisTemplate.opsForValue().set(key, order);

        rabbitTemplate.convertAndSend(RabbitMQConfig.UPDATED_ORDER_EXCHANGE,
                RabbitMQConfig.UPDATED_ORDER_ROUTING_KEY, order);

        logger.info("Enviado o pedido {} para fila de status do pedido com o status {}", order.getId(), order.getStatus());
        return order;
    }

    public Order getOrder(String orderId) {
        return (Order) redisTemplate.opsForValue().get(ORDER_KEY_PREFIX + orderId);
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
