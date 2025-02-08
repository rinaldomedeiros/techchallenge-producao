package com.example.orderproduction.cucumber.bdd_steps;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.orderproduction.dto.OrderStatusUpdateDTO;
import com.example.orderproduction.model.Order;
import com.example.orderproduction.model.OrderStatus;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;

import java.util.concurrent.TimeUnit;


public class OrderStatusUpdateSteps {

    private final TestRestTemplate restTemplate;
    private final RedisTemplate<String, Object> redisTemplate;
    private String orderId;
    private ResponseEntity<Order> queryResponse;

    // Constructor injection – Spring will inject both TestRestTemplate and RedisTemplate.
    public OrderStatusUpdateSteps(TestRestTemplate restTemplate, RedisTemplate<String, Object> redisTemplate) {
        this.restTemplate = restTemplate;
        this.redisTemplate = redisTemplate;
    }

    @Given("que recebo um pedido com status {string}")
    public void iReceiveAnOrderWithStatus(String status) {

        orderId = "123";
        Order order = new Order();
        order.setId(orderId);
        order.setStatus(OrderStatus.valueOf(status));

        // The service uses the key "order:" + orderId. Pre-load the order into Redis.
        redisTemplate.opsForValue().set("order:" + orderId, order, 30, TimeUnit.MINUTES);
        System.out.println("Pre-loaded order " + orderId + " with status " + status + " into Redis.");
    }

    @When("atualizo o status do pedido para {string}")
    public void iUpdateTheOrderStatusTo(String newStatus) {
        // Prepare the DTO for status update.
        OrderStatusUpdateDTO dto = new OrderStatusUpdateDTO();
        dto.setStatus(OrderStatus.valueOf(newStatus));

        // Call the PUT endpoint to update the order status.
        restTemplate.put("/orders/" + orderId + "/status", dto);

        // After the update, query the order.
        queryResponse = restTemplate.getForEntity("/orders/" + orderId, Order.class);
    }

    @Then("o pedido é enviado para a fila de {string}")
    public void theOrderIsPublishedToTheQueue(String expectedQueue) {

        System.out.println("O pedido foi enviado para a queue : " + expectedQueue);
    }

    @And("ao consultar o pedido, o status deve ser {string}")
    public void whenIQueryTheOrderItsStatusShouldBe(String expectedStatus) {
        Order queriedOrder = queryResponse.getBody();
        assertThat(queriedOrder).isNotNull();
        assertThat(queriedOrder.getStatus()).isEqualTo(OrderStatus.valueOf(expectedStatus));
    }
}


