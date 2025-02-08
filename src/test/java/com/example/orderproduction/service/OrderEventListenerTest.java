package com.example.orderproduction.service;

import com.example.orderproduction.model.Order;
import com.example.orderproduction.model.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.mockito.Mockito.verify;

public class OrderEventListenerTest {

    private OrderService orderService;
    private OrderEventListener orderEventListener;

    @BeforeEach
    public void setUp() {

        orderService = Mockito.mock(OrderService.class);

        orderEventListener = new OrderEventListener(orderService);
    }

    @Test
    public void testReceiveOrder() {

        Order order = new Order("1", OrderStatus.RECEBIDO, "Detalhes do pedido");

        orderEventListener.receiveOrder(order);

        verify(orderService).processOrder(order);
    }
}
