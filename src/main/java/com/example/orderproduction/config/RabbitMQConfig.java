package com.example.orderproduction.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // Fila e exchange para pedidos pagos
    public static final String PAID_ORDER_QUEUE = "paid.order.queue";
    public static final String PAID_ORDER_EXCHANGE = "paid.order.exchange";
    public static final String PAID_ORDER_ROUTING_KEY = "new.order";

    // Fila e exchange para atualizações de pedidos
    public static final String UPDATED_ORDER_QUEUE = "updated.order.queue";
    public static final String UPDATED_ORDER_EXCHANGE = "updated.order.exchange";
    public static final String UPDATED_ORDER_ROUTING_KEY = "updated.order";

    @Bean
    public Queue paidOrderQueue() {
        return QueueBuilder.durable(PAID_ORDER_QUEUE).build();
    }

    @Bean
    public TopicExchange paidOrderExchange() {
        return new TopicExchange(PAID_ORDER_EXCHANGE);
    }

    @Bean
    public Binding bindingPaidOrder(Queue paidOrderQueue, TopicExchange paidOrderExchange) {
        return BindingBuilder.bind(paidOrderQueue).to(paidOrderExchange).with(PAID_ORDER_ROUTING_KEY);
    }

    @Bean
    public Queue updatedOrderQueue() {
        return QueueBuilder.durable(UPDATED_ORDER_QUEUE).build();
    }

    @Bean
    public TopicExchange updatedOrderExchange() {
        return new TopicExchange(UPDATED_ORDER_EXCHANGE);
    }

    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public Binding bindingUpdatedOrder(Queue updatedOrderQueue, TopicExchange updatedOrderExchange) {
        return BindingBuilder.bind(updatedOrderQueue).to(updatedOrderExchange).with(UPDATED_ORDER_ROUTING_KEY);
    }


    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }

}
