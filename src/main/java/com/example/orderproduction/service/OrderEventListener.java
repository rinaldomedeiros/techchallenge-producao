package com.example.orderproduction.service;

import com.example.orderproduction.config.RabbitMQConfig;
import com.example.orderproduction.model.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class OrderEventListener {

    private static final Logger logger = LoggerFactory.getLogger(OrderEventListener.class);

    private final OrderService orderService;

    public OrderEventListener(OrderService orderService) {
        this.orderService = orderService;
    }


    @RabbitListener(queues = RabbitMQConfig.CONFIRMED_ORDER_QUEUE)
    public void receiveOrder(Order order) {
        orderService.processOrder(order);

        logger.info("recebido objeto pedido da FILA CONFIRMED_ORDER_QUEUE :" + order);

    }
}
