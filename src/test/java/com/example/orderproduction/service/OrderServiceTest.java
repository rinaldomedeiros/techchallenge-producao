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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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

    // Testes para processOrder()

    @Test
    public void testProcessOrder_SetsStatusIfNull() {
        // Arrange: Cria um pedido sem status definido (null)
        Order order = new Order("123", null, "Test details");

        // Act: Processa o pedido
        orderService.processOrder(order);

        // Assert: Verifica se o status foi definido para RECEBIDO
        assertEquals(OrderStatus.RECEBIDO, order.getStatus());
        // Verifica se a operação de set foi chamada com os parâmetros corretos
        verify(valueOperations, times(1))
                .set("order:" + order.getId(), order, 30, TimeUnit.MINUTES);
    }

    @Test
    public void testProcessOrder_KeepsExistingStatus() {
        // Arrange: Cria um pedido com status já definido
        Order order = new Order("124", OrderStatus.EM_PREPARACAO, "Test details");

        // Act: Processa o pedido
        orderService.processOrder(order);

        // Assert: O status existente deve ser mantido
        assertEquals(OrderStatus.EM_PREPARACAO, order.getStatus());
        verify(valueOperations).set("order:" + order.getId(), order, 30, TimeUnit.MINUTES);
    }

    // Testes para updateOrderStatus()

    @Test
    public void testUpdateOrderStatus_Success() {
        // Arrange: Cria um pedido existente com status RECEBIDO
        String orderId = "123";
        Order order = new Order(orderId, OrderStatus.RECEBIDO, "Test details");
        OrderStatus newStatus = OrderStatus.EM_PREPARACAO;
        String key = ORDER_KEY_PREFIX + orderId;
        when(valueOperations.get(key)).thenReturn(order);

        // Act: Atualiza o status do pedido
        Order updatedOrder = orderService.updateOrderStatus(orderId, newStatus);

        // Assert: Verifica se o status foi atualizado e se as operações foram chamadas
        assertEquals(newStatus, updatedOrder.getStatus());
        verify(valueOperations).get(key);
        verify(valueOperations).set(key, order);
        verify(rabbitTemplate).convertAndSend(
                RabbitMQConfig.UPDATED_ORDER_EXCHANGE,
                RabbitMQConfig.UPDATED_ORDER_ROUTING_KEY,
                order);
    }

    @Test
    public void testUpdateOrderStatus_OrderNotFound() {
        // Arrange: Simula a ausência do pedido no Redis
        String orderId = "123";
        OrderStatus newStatus = OrderStatus.EM_PREPARACAO;
        String key = ORDER_KEY_PREFIX + orderId;
        when(valueOperations.get(key)).thenReturn(null);

        // Act & Assert: Espera que uma exceção seja lançada
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                orderService.updateOrderStatus(orderId, newStatus)
        );
        assertEquals("Pedido não encontrado com id: " + orderId, exception.getMessage());
    }

    // Testes para getOrder()

    @Test
    public void testGetOrder_Found() {
        // Arrange: Cria um pedido e simula sua existência no Redis
        String orderId = "123";
        Order order = new Order(orderId, OrderStatus.RECEBIDO, "Test details");
        when(valueOperations.get("order:" + orderId)).thenReturn(order);

        // Act: Recupera o pedido
        Order result = orderService.getOrder(orderId);

        // Assert: Verifica se o pedido retornado não é nulo e possui o id esperado
        assertNotNull(result);
        assertEquals(orderId, result.getId());
    }

    @Test
    public void testGetOrder_NotFound() {
        // Arrange: Simula que nenhum pedido foi encontrado
        String orderId = "123";
        when(valueOperations.get("order:" + orderId)).thenReturn(null);

        // Act: Tenta recuperar o pedido
        Order result = orderService.getOrder(orderId);

        // Assert: O retorno deve ser nulo
        assertNull(result);
    }

    // Testes para getOrdersByStatus()

    @Test
    public void testGetOrdersByStatus_Found() {
        // Arrange: Cria três pedidos com status diferentes
        OrderStatus desiredStatus = OrderStatus.RECEBIDO;
        Order order1 = new Order("1", OrderStatus.RECEBIDO, "Details 1");
        Order order2 = new Order("2", OrderStatus.EM_PREPARACAO, "Details 2");
        Order order3 = new Order("3", OrderStatus.RECEBIDO, "Details 3");

        Set<String> keys = new HashSet<>(Arrays.asList("order:1", "order:2", "order:3"));
        when(redisTemplate.keys("order:*")).thenReturn(keys);
        when(valueOperations.get("order:1")).thenReturn(order1);
        when(valueOperations.get("order:2")).thenReturn(order2);
        when(valueOperations.get("order:3")).thenReturn(order3);

        // Act: Busca os pedidos com o status desejado
        List<Order> orders = orderService.getOrdersByStatus(desiredStatus);

        // Assert: Verifica se apenas os pedidos com o status RECEBIDO foram retornados
        assertEquals(2, orders.size());
        assertTrue(orders.contains(order1));
        assertTrue(orders.contains(order3));
    }

    @Test
    public void testGetOrdersByStatus_NoMatchingOrders() {
        // Arrange: Cria pedidos que não possuem o status desejado
        OrderStatus desiredStatus = OrderStatus.RECEBIDO;
        Order order1 = new Order("1", OrderStatus.EM_PREPARACAO, "Details 1");
        Order order2 = new Order("2", OrderStatus.EM_PREPARACAO, "Details 2");

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
        OrderStatus desiredStatus = OrderStatus.RECEBIDO;
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
        OrderStatus desiredStatus = OrderStatus.RECEBIDO;
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
        OrderStatus desiredStatus = OrderStatus.RECEBIDO;
        Order order1 = new Order("1", OrderStatus.RECEBIDO, "Details 1");
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
