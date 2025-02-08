package com.example.orderproduction.controller;

import com.example.orderproduction.dto.OrderStatusUpdateDTO;
import com.example.orderproduction.model.Order;
import com.example.orderproduction.model.OrderStatus;
import com.example.orderproduction.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class OrderControllerTest {

    private MockMvc mockMvc;
    private OrderService orderService;
    private ObjectMapper objectMapper;

    @BeforeEach
    public void setUp() {
        // Cria o mock manualmente para o OrderService
        orderService = Mockito.mock(OrderService.class);

        // Instancia a controller com o serviço mockado
        OrderController orderController = new OrderController(orderService);

        // Configura o MockMvc em modo standalone
        mockMvc = MockMvcBuilders.standaloneSetup(orderController).build();

        // Instancia o ObjectMapper manualmente
        objectMapper = new ObjectMapper();
    }

    @Test
    public void testUpdateStatus() throws Exception {
        String orderId = "123";
        OrderStatus newStatus = OrderStatus.EM_PREPARACAO;
        Order updatedOrder = new Order(orderId, newStatus, "Order details");

        // Configura o comportamento do serviço mockado
        when(orderService.updateOrderStatus(orderId, newStatus)).thenReturn(updatedOrder);

        OrderStatusUpdateDTO request = new OrderStatusUpdateDTO();
        request.setStatus(newStatus);

        // Executa a requisição PUT e valida a resposta
        mockMvc.perform(put("/orders/" + orderId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId))
                .andExpect(jsonPath("$.status").value(newStatus.toString()));
    }

    @Test
    public void testGetOrderFound() throws Exception {
        String orderId = "123";
        Order order = new Order(orderId, OrderStatus.RECEBIDO, "Test details");

        // Simula que o pedido foi encontrado
        when(orderService.getOrder(orderId)).thenReturn(order);

        // Executa a requisição GET e valida a resposta
        mockMvc.perform(get("/orders/" + orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId))
                .andExpect(jsonPath("$.status").value(OrderStatus.RECEBIDO.toString()));
    }

    @Test
    public void testGetOrderNotFound() throws Exception {
        String orderId = "123";

        // Simula que o pedido não foi encontrado
        when(orderService.getOrder(orderId)).thenReturn(null);

        // Executa a requisição GET e espera o status 404 (Not Found)
        mockMvc.perform(get("/orders/" + orderId))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testGetOrdersByStatusFound() throws Exception {
        OrderStatus status = OrderStatus.RECEBIDO;
        Order order1 = new Order("1", status, "Details 1");
        Order order2 = new Order("2", status, "Details 2");
        List<Order> orders = Arrays.asList(order1, order2);

        // Simula que a consulta retorna dois pedidos
        when(orderService.getOrdersByStatus(status)).thenReturn(orders);

        // Executa a requisição GET passando o parâmetro de status e valida a resposta
        mockMvc.perform(get("/orders/status")
                        .param("status", status.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("1"))
                .andExpect(jsonPath("$[1].id").value("2"));
    }

    @Test
    public void testGetOrdersByStatusNoContent() throws Exception {
        OrderStatus status = OrderStatus.EM_PREPARACAO;

        // Simula que não há pedidos com o status informado
        when(orderService.getOrdersByStatus(status)).thenReturn(Collections.emptyList());

        // Executa a requisição GET e espera o status 204 (No Content)
        mockMvc.perform(get("/orders/status")
                        .param("status", status.toString()))
                .andExpect(status().isNoContent());
    }
}