package com.example.cafe.controller;

import com.example.cafe.service.OrderService;
import com.example.cafe.model.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping
    public List<Order> getAllOrders() {
        return orderService.getAllOrders();
    }

    @PostMapping
    public Order placeOrder(@RequestBody Order order) {
        return orderService.placeOrder(order);
    }

    @GetMapping("/by-customer")
    public List<Order> getOrdersByCustomer(@RequestParam String customerName) {
        return orderService.getOrdersByCustomer(customerName);
    }
}
