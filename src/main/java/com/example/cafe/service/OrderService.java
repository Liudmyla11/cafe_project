package com.example.cafe.service;

import com.example.cafe.repository.OrderRepository;
import com.example.cafe.model.Order;
import com.example.cafe.model.MenuItem;
import com.example.cafe.model.Cafe;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final CafeService cafeService;
    private final MenuItemService menuItemService;

    public Order placeOrder(Order order) {
        if (order.getCafe() != null && order.getCafe().getId() != null) {
            Cafe cafe = cafeService.getCafeById(order.getCafe().getId());
            if (cafe == null) {
                throw new IllegalArgumentException("Cafe with ID " + order.getCafe().getId() + " not found");
            }
            order.setCafe(cafe);
        } else {
            throw new IllegalArgumentException("Order must contain a valid cafe with an ID");
        }

        if (order.getItems() != null && !order.getItems().isEmpty()) {
            List<MenuItem> fullItems = order.getItems().stream()
                    .map(item -> menuItemService.getById(item.getId()))
                    .collect(Collectors.toList());

            for (MenuItem item : fullItems) {
                if (!item.getCafe().equals(order.getCafe())) {
                    throw new IllegalArgumentException("MenuItem " + item.getName() + " does not belong to the selected cafe");
                }
            }

            order.setItems(fullItems);
        } else {
            throw new IllegalArgumentException("Order must contain at least one menu item");
        }

        return orderRepository.save(order);
    }

    public List<Order> getOrdersByCustomer(String customerName) {
        return orderRepository.findByCustomerName(customerName);
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }
}