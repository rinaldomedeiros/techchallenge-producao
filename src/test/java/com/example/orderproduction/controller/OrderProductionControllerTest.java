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

public class OrderProductionControllerTest {

    private MockMvc mockMvc;
    private OrderService orderService;
    private ObjectMapper objectMapper;

    @BeforeEach
    public void setUp() {

        orderService = Mockito.mock(OrderService.class);

        OrderProductionController orderProductionController = new OrderProductionController(orderService);

        mockMvc = MockMvcBuilders.standaloneSetup(orderProductionController).build();

        objectMapper = new ObjectMapper();
    }

    @Test
    public void testUpdateStatus() throws Exception {
        int orderId = 123;
        OrderStatus newStatus = OrderStatus.IN_PREPARATION;
        Order updatedOrder = new Order(orderId, newStatus, "Order details");

        // Configura o comportamento do serviço mockado
        when(orderService.updateOrderStatus(orderId, newStatus)).thenReturn(updatedOrder);

        OrderStatusUpdateDTO request = new OrderStatusUpdateDTO();
        request.setStatus(newStatus);

        // Executa a requisição PUT e valida a resposta
        mockMvc.perform(put("/order-production/orders/" + orderId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(orderId))
                .andExpect(jsonPath("$.status").value(newStatus.toString()));
    }

    @Test
    public void testGetOrderFound() throws Exception {
        int orderId = 123;
        Order order = new Order(orderId, OrderStatus.RECEIVED, "Test details");

        when(orderService.getOrder(orderId)).thenReturn(order);

        mockMvc.perform(get("/order-production/orders/" + orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(orderId))
                .andExpect(jsonPath("$.status").value(OrderStatus.RECEIVED.toString()));
    }

    @Test
    public void testGetOrderNotFound() throws Exception {
       int orderId = 123;

        when(orderService.getOrder(orderId)).thenReturn(null);

        mockMvc.perform(get("/order-production/orders/" + orderId))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testGetOrdersByStatusFound() throws Exception {
        OrderStatus status = OrderStatus.RECEIVED;
        Order order1 = new Order(1, status, "Details 1");
        Order order2 = new Order(2, status, "Details 2");
        List<Order> orders = Arrays.asList(order1, order2);

        when(orderService.getOrdersByStatus(status)).thenReturn(orders);

        mockMvc.perform(get("/order-production/orders/status")
                        .param("status", status.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].orderId").value("1"))
                .andExpect(jsonPath("$[1].orderId").value("2"));
    }

    @Test
    public void testGetOrdersByStatusNotFound() throws Exception {
        OrderStatus status = OrderStatus.IN_PREPARATION;

        when(orderService.getOrdersByStatus(status)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/order-production/orders/status")
                        .param("status", status.toString()))
                .andExpect(status().isNotFound());
    }
}