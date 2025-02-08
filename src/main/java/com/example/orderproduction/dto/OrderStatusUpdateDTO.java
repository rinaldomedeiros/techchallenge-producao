package com.example.orderproduction.dto;

import com.example.orderproduction.model.OrderStatus;

public class OrderStatusUpdateDTO {

    private OrderStatus status;

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }
}