package com.example.orderproduction.producer;

import com.example.orderproduction.config.RabbitMQConfig;
import com.example.orderproduction.model.Order;
import com.example.orderproduction.model.OrderStatus;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ProducerService {

    private final RabbitTemplate rabbitTemplate;

    public ProducerService(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public Order produceOrder(Object details) {
        Order order = new Order();
        order.setId(UUID.randomUUID().toString());
        order.setStatus(OrderStatus.RECEBIDO);
        order.setDetails(details);
        // Publica a mensagem na exchange de pedidos pagos
        rabbitTemplate.convertAndSend(RabbitMQConfig.PAID_ORDER_EXCHANGE,
                RabbitMQConfig.PAID_ORDER_ROUTING_KEY, order);
        return order;
    }
}
