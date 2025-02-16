package com.example.orderproduction.service;

import com.example.orderproduction.config.RabbitMQConfig;
import com.example.orderproduction.model.Order;
import com.example.orderproduction.model.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OrderServiceTest {

    private static final String ORDER_KEY_PREFIX = "order:";

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private OrderService orderService;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @BeforeEach
    public void setUp() {

        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }


    @Test
    public void testProcessOrder_SetsStatusIfNull() {

        Order order = new Order(123, null, "Test details");

        orderService.processOrder(order);

        assertEquals(OrderStatus.RECEIVED, order.getStatus());

        verify(valueOperations, times(1))
                .set("order:" + order.getOrderId(), order, 30, TimeUnit.MINUTES);
    }

    @Test
    public void testProcessOrder_KeepsExistingStatus() {

        Order order = new Order(124, OrderStatus.IN_PREPARATION, "Test details");

        orderService.processOrder(order);

        assertEquals(OrderStatus.IN_PREPARATION, order.getStatus());
        verify(valueOperations).set("order:" + order.getOrderId(), order, 30, TimeUnit.MINUTES);
    }

    @Test
    public void testUpdateOrderStatus_Success() {

        int orderId = 123;
        Order order = new Order(orderId, OrderStatus.RECEIVED, "Test details");
        OrderStatus newStatus = OrderStatus.IN_PREPARATION;
        String key = ORDER_KEY_PREFIX + orderId;
        when(valueOperations.get(key)).thenReturn(order);

        Order updatedOrder = orderService.updateOrderStatus(orderId, newStatus);

        Map<String, Object> updatedOrderMessage = new HashMap<>();
        updatedOrderMessage.put("orderId", order.getOrderId());
        updatedOrderMessage.put("orderStatus", order.getStatus());

        assertEquals(newStatus, updatedOrder.getStatus());


        assertNotNull(updatedOrderMessage);

        assertTrue(updatedOrderMessage instanceof Map);

        assertEquals(2, updatedOrderMessage.size());

        assertTrue(updatedOrderMessage.containsKey("orderId"));
        assertEquals(123, updatedOrderMessage.get("orderId"));

        assertTrue(updatedOrderMessage.containsKey("orderStatus"));
        assertEquals(OrderStatus.IN_PREPARATION, updatedOrderMessage.get("orderStatus"));

        verify(valueOperations).get(key);
        verify(valueOperations).set(key, order);
        verify(rabbitTemplate).convertAndSend(
                RabbitMQConfig.UPDATED_ORDER_EXCHANGE,
                RabbitMQConfig.UPDATED_ORDER_ROUTING_KEY,
                updatedOrderMessage);
    }

    @Test
    public void testUpdateOrderStatus_OrderNotFound() {

        int orderId = 123;
        OrderStatus newStatus = OrderStatus.IN_PREPARATION;
        String key = ORDER_KEY_PREFIX + orderId;
        when(valueOperations.get(key)).thenReturn(null);

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                orderService.updateOrderStatus(orderId, newStatus)
        );
        assertEquals("Pedido não encontrado com id: " + orderId, exception.getMessage());
    }


    @Test
    public void testGetOrder_Found() {

        int orderId = 123;
        Order order = new Order(orderId, OrderStatus.RECEIVED, "Test details");
        when(valueOperations.get("order:" + orderId)).thenReturn(order);

        Order result = orderService.getOrder(orderId);

        assertNotNull(result);
        assertEquals(orderId, result.getOrderId());
    }

    @Test
    public void testGetOrder_NotFound() {
        int orderId = 123;
        when(valueOperations.get("order:" + orderId)).thenReturn(null);

        Order result = orderService.getOrder(orderId);

        assertNull(result);
    }


    @Test
    public void testGetOrdersByStatus_Found() {

        OrderStatus desiredStatus = OrderStatus.RECEIVED;
        Order order1 = new Order(1, OrderStatus.RECEIVED, "Details 1");
        Order order2 = new Order(2, OrderStatus.IN_PREPARATION, "Details 2");
        Order order3 = new Order(3, OrderStatus.RECEIVED, "Details 3");

        Set<String> keys = new HashSet<>(Arrays.asList("order:1", "order:2", "order:3"));
        when(redisTemplate.keys("order:*")).thenReturn(keys);
        when(valueOperations.get("order:1")).thenReturn(order1);
        when(valueOperations.get("order:2")).thenReturn(order2);
        when(valueOperations.get("order:3")).thenReturn(order3);

        List<Order> orders = orderService.getOrdersByStatus(desiredStatus);

        assertEquals(2, orders.size());
        assertTrue(orders.contains(order1));
        assertTrue(orders.contains(order3));
    }

    @Test
    public void testGetOrdersByStatus_NoMatchingOrders() {
        // Arrange: Cria pedidos que não possuem o status desejado
        OrderStatus desiredStatus = OrderStatus.RECEIVED;
        Order order1 = new Order(1, OrderStatus.IN_PREPARATION, "Details 1");
        Order order2 = new Order(2, OrderStatus.IN_PREPARATION, "Details 2");

        Set<String> keys = new HashSet<>(Arrays.asList("order:1", "order:2"));
        when(redisTemplate.keys("order:*")).thenReturn(keys);
        when(valueOperations.get("order:1")).thenReturn(order1);
        when(valueOperations.get("order:2")).thenReturn(order2);

        // Act: Busca os pedidos com o status desejado
        List<Order> orders = orderService.getOrdersByStatus(desiredStatus);

        // Assert: Como nenhum pedido possui o status RECEBIDO, a lista deve estar vazia
        assertTrue(orders.isEmpty());
    }

    @Test
    public void testGetOrdersByStatus_NullKeys() {
        // Arrange: Simula que redisTemplate.keys(...) retorna null
        OrderStatus desiredStatus = OrderStatus.RECEIVED;
        when(redisTemplate.keys("order:*")).thenReturn(null);

        // Act: Chama o método
        List<Order> orders = orderService.getOrdersByStatus(desiredStatus);

        // Assert: A lista retornada deve ser vazia (mas não nula)
        assertNotNull(orders);
        assertTrue(orders.isEmpty());
    }

    @Test
    public void testGetOrdersByStatus_EmptyKeys() {
        // Arrange: Simula que redisTemplate.keys(...) retorna um conjunto vazio
        OrderStatus desiredStatus = OrderStatus.RECEIVED;
        when(redisTemplate.keys("order:*")).thenReturn(Collections.emptySet());

        // Act: Chama o método
        List<Order> orders = orderService.getOrdersByStatus(desiredStatus);

        // Assert: A lista retornada deve ser vazia
        assertNotNull(orders);
        assertTrue(orders.isEmpty());
    }

    @Test
    public void testGetOrdersByStatus_SomeNullOrders() {
        // Arrange: Cria um cenário onde algumas chaves retornam pedidos nulos
        OrderStatus desiredStatus = OrderStatus.RECEIVED;
        Order order1 = new Order(1, OrderStatus.RECEIVED, "Details 1");
        Order order2 = null;  // Simula pedido não encontrado
        Set<String> keys = new HashSet<>(Arrays.asList("order:1", "order:2"));
        when(redisTemplate.keys("order:*")).thenReturn(keys);
        when(valueOperations.get("order:1")).thenReturn(order1);
        when(valueOperations.get("order:2")).thenReturn(order2);

        // Act: Chama o método
        List<Order> orders = orderService.getOrdersByStatus(desiredStatus);

        // Assert: Apenas order1 deve ser adicionado à lista
        assertNotNull(orders);
        assertEquals(1, orders.size());
        assertTrue(orders.contains(order1));
    }
}
