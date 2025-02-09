package com.example.orderproduction.cucumber.bdd_steps;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.example.orderproduction.config.RabbitMQConfig;
import com.example.orderproduction.dto.OrderStatusUpdateDTO;
import com.example.orderproduction.model.Order;
import com.example.orderproduction.model.OrderStatus;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;

public class OrderStatusUpdateSteps {

    private final TestRestTemplate restTemplate;
    private final RedisTemplate<String, Object> redisTemplate;

    @Mock
    private RabbitTemplate rabbitTemplate;

    private String orderId;
    private ResponseEntity<Order> queryResponse;

    public OrderStatusUpdateSteps(TestRestTemplate restTemplate, RedisTemplate<String, Object> redisTemplate) {
        this.restTemplate = restTemplate;
        this.redisTemplate = redisTemplate;
    }

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Given("que recebo um pedido com status {string}")
    public void iReceiveAnOrderWithStatus(String status) {
        orderId = "123";
        Order order = new Order();
        order.setId(orderId);
        order.setStatus(OrderStatus.valueOf(status));

        redisTemplate.opsForValue().set("order:" + orderId, order, 30, TimeUnit.MINUTES);
        System.out.println("Pedido " + orderId + " pré-carregado com status " + status + " no Redis.");
    }

    @When("atualizo o status do pedido para {string}")
    public void iUpdateTheOrderStatusTo(String newStatus) {
        OrderStatusUpdateDTO dto = new OrderStatusUpdateDTO();
        dto.setStatus(OrderStatus.valueOf(newStatus));

        restTemplate.put("/orders/" + orderId + "/status", dto);
        queryResponse = restTemplate.getForEntity("/orders/" + orderId, Order.class);
    }

    @Then("o pedido é enviado para a fila de pedidos atualizados")
    public void theOrderIsPublishedToTheQueue() {

        rabbitTemplate.setMessageConverter(new Jackson2JsonMessageConverter());

        Map<String, Object> message = new HashMap<>();
        message.put("orderId", orderId);
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.UPDATED_ORDER_EXCHANGE,
                RabbitMQConfig.UPDATED_ORDER_ROUTING_KEY,
                message
        );

        verify(rabbitTemplate, times(1)).convertAndSend(
                eq(RabbitMQConfig.UPDATED_ORDER_EXCHANGE),
                eq(RabbitMQConfig.UPDATED_ORDER_ROUTING_KEY),
                any(Map.class)
        );

        System.out.println("Mensagem enviada para a exchange '" + RabbitMQConfig.UPDATED_ORDER_EXCHANGE +
                "' com a routing key '" + RabbitMQConfig.UPDATED_ORDER_ROUTING_KEY + "'.");
    }

    @And("ao consultar o pedido, o status deve ser {string}")
    public void whenIQueryTheOrderItsStatusShouldBe(String expectedStatus) {
        assertThat(queryResponse.getStatusCode().is2xxSuccessful())
                .isTrue();

        Order queriedOrder = queryResponse.getBody();
        assertThat(queriedOrder)
                .isNotNull();

        assertThat(queriedOrder.getStatus())
                .isEqualTo(OrderStatus.valueOf(expectedStatus));
    }
}
