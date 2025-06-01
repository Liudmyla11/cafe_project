package com.example.cafe.controller;

import com.example.cafe.model.Cafe;
import com.example.cafe.model.MenuItem;
import com.example.cafe.model.Order;
import com.example.cafe.model.User;
import com.example.cafe.repository.CafeRepository;
import com.example.cafe.repository.MenuItemRepository;
import com.example.cafe.repository.OrderRepository;
import com.example.cafe.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

import java.util.*;

@RestController
@RequestMapping("/customer")
@PreAuthorize("hasRole('CUSTOMER')")
public class CustomerController {

    private static final Logger logger = LoggerFactory.getLogger(CustomerController.class);

    private final CafeRepository cafeRepository;
    private final MenuItemRepository menuItemRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    @Autowired
    public CustomerController(CafeRepository cafeRepository,
                              MenuItemRepository menuItemRepository,
                              OrderRepository orderRepository,
                              UserRepository userRepository) {
        this.cafeRepository = cafeRepository;
        this.menuItemRepository = menuItemRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/cafes")
    public ResponseEntity<List<Cafe>> getAllCafes() {
        logger.info("Отримання списку всіх кафе");
        return ResponseEntity.ok(cafeRepository.findAll());
    }

    @GetMapping("/cafes/{cafeId}/menu")
    public ResponseEntity<List<MenuItem>> getMenuByCafe(@PathVariable Long cafeId) {
        logger.info("Отримання меню для кафе з ID={}", cafeId);
        if (!cafeRepository.existsById(cafeId)) {
            logger.warn("Кафе з ID={} не знайдено", cafeId);
            return ResponseEntity.notFound().build();
        }
        List<MenuItem> menu = menuItemRepository.findByCafeId(cafeId);
        logger.info("Знайдено {} позицій у меню кафе ID={}", menu.size(), cafeId);
        return ResponseEntity.ok(menuItemRepository.findByCafeId(cafeId));
    }

    @PostMapping("/orders")
    public ResponseEntity<?> createOrder(@RequestBody Order orderRequest, Authentication authentication) {
        String username = authentication.getName();
        logger.info("Спроба створити замовлення користувачем '{}'", username);

        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    logger.error("Користувача '{}' не знайдено у базі", username);
                    return new UsernameNotFoundException("Користувача не знайдено");
                });

        if (orderRequest.getCafe() == null || orderRequest.getCafe().getId() == null) {
            logger.warn("Замовлення не містить ID кафе");
            return ResponseEntity.badRequest().body("Кафе є обов’язковим");
        }

        Optional<Cafe> cafeOpt = cafeRepository.findById(orderRequest.getCafe().getId());
        if (cafeOpt.isEmpty()) {
            logger.warn("Кафе з ID={} не знайдено", orderRequest.getCafe().getId());
            return ResponseEntity.badRequest().body("Кафе не знайдено");
        }

        Cafe cafe = cafeOpt.get();

        if (orderRequest.getItems() == null || orderRequest.getItems().isEmpty()) {
            logger.warn("Замовлення не містить позицій");
            return ResponseEntity.badRequest().body("Список позицій замовлення не може бути порожнім");
        }

        List<MenuItem> validItems = new ArrayList<>();
        for (MenuItem item : orderRequest.getItems()) {
            Optional<MenuItem> itemOpt = menuItemRepository.findById(item.getId());
            if (itemOpt.isEmpty()) {
                logger.warn("Позицію меню з ID={} не знайдено", item.getId());
                return ResponseEntity.badRequest().body("Позицію меню не знайдено: ID = " + item.getId());
            }
            if (!itemOpt.get().getCafe().getId().equals(cafe.getId())) {
                logger.warn("Позиція меню ID={} не належить до кафе ID={}", item.getId(), cafe.getId());
                return ResponseEntity.badRequest().body("Позиція не належить до вибраного кафе");
            }
            validItems.add(itemOpt.get());
        }

        Order newOrder = new Order();
        newOrder.setCafe(cafe);
        newOrder.setItems(validItems);
        newOrder.setUser(currentUser);

        double totalAmount = validItems.stream()
                .mapToDouble(MenuItem::getPrice)
                .sum();
        newOrder.setTotalAmount(totalAmount);

        Order savedOrder = orderRepository.save(newOrder);
        logger.info("Замовлення ID={} створено користувачем '{}', сума: {}", savedOrder.getId(), username, totalAmount);

        return ResponseEntity.ok(savedOrder);
    }

    @GetMapping("/orders")
    public ResponseEntity<List<Map<String, Object>>> getUserOrders(Authentication authentication) {
        String username = authentication.getName();
        logger.info("Отримання замовлень для користувача '{}'", username);

        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    logger.error("Користувача '{}' не знайдено", username);
                    return new UsernameNotFoundException("Користувача не знайдено");
                });

        List<Order> orders = orderRepository.findByUserId(currentUser.getId());

        List<Map<String, Object>> response = new ArrayList<>();
        for (Order order : orders) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", order.getId());
            map.put("cafeName", order.getCafe().getName());
            map.put("totalAmount", order.getTotalAmount());

            List<String> itemNames = order.getItems()
                    .stream()
                    .map(MenuItem::getName)
                    .toList();
            map.put("itemNames", itemNames);

            response.add(map);
        }

        logger.info("Знайдено {} замовлень для користувача '{}'", response.size(), username);
        return ResponseEntity.ok(response);
    }
}