package com.example.orderproduction.service;

import com.example.orderproduction.config.RabbitMQConfig;
import com.example.orderproduction.model.Order;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class OrderEventListener {

    private final OrderService orderService;

    public OrderEventListener(OrderService orderService) {
        this.orderService = orderService;
    }

    // Escuta eventos de pedidos novos que chegam na fila de pedidos pagos
    @RabbitListener(queues = RabbitMQConfig.PAID_ORDER_QUEUE)
    public void receiveOrder(Order order) {
        orderService.processOrder(order);
    }
}
