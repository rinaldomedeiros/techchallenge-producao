//package com.example.orderproduction.producer;
//
//import com.example.orderproduction.config.RabbitMQConfig;
//import com.example.orderproduction.model.Order;
//import com.example.orderproduction.model.OrderStatus;
//import org.springframework.amqp.rabbit.core.RabbitTemplate;
//import org.springframework.stereotype.Service;
//
//import java.util.UUID;
//
//import static java.lang.Math.random;
//
//@Service
//public class ProducerService {
//
//    private final RabbitTemplate rabbitTemplate;
//
//    public ProducerService(RabbitTemplate rabbitTemplate) {
//        this.rabbitTemplate = rabbitTemplate;
//    }
//
//    public Order produceOrder(Object details) {
//        Order order = new Order();
//        order.setId((int) random());
//        order.setStatus(OrderStatus.RECEIVED);
//        order.setDetails(details);
//
//        rabbitTemplate.convertAndSend(RabbitMQConfig.CONFIRMED_ORDER_EXCHANGE,
//                RabbitMQConfig.CONFIRMED_ORDER_ROUTING_KEY, order);
//        return order;
//    }
//}
