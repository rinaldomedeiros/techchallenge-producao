package com.example.orderproduction.controller;

import com.example.orderproduction.dto.OrderStatusUpdateDTO;
import com.example.orderproduction.model.Order;
import com.example.orderproduction.model.OrderStatus;
import com.example.orderproduction.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/order-production/orders")
public class OrderProductionController {

    private final OrderService orderService;

    public OrderProductionController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Order> updateStatus(@PathVariable("id") int orderId,
                                              @RequestBody OrderStatusUpdateDTO request) {
        try {
            Order updatedOrder = orderService.updateOrderStatus(orderId, request.getStatus());
            return ResponseEntity.ok(updatedOrder);
        } catch (RuntimeException e) {

            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrder(@PathVariable("id") int orderId) {
        Order order = orderService.getOrder(orderId);
        if (order == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(order);
    }

    @GetMapping("/status")
    public ResponseEntity<List<Order>> getOrdersByStatus(@RequestParam("status") OrderStatus status) {
        List<Order> orders = orderService.getOrdersByStatus(status);
        if (orders.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(orders);
    }
}
