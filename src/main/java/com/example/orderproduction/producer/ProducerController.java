package com.example.orderproduction.producer;

import com.example.orderproduction.model.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/producer")
public class ProducerController {

    private final ProducerService producerService;

    public ProducerController(ProducerService producerService) {
        this.producerService = producerService;
    }

    // Endpoint para produzir um pedido de teste
    @PostMapping("/order")
    public ResponseEntity<Order> produceOrder(@RequestBody ProducerRequest request) {
        Order order = producerService.produceOrder(request.getDetails());
        return ResponseEntity.ok(order);
    }

    // DTO para a requisição de produção
    public static class ProducerRequest {
        private String details;

        public String getDetails() {
            return details;
        }
        public void setDetails(String details) {
            this.details = details;
        }
    }
}
