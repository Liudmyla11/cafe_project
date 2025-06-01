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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/manager")
@PreAuthorize("hasRole('MANAGER')")
public class ManagerController {

    private static final Logger logger = LoggerFactory.getLogger(ManagerController.class);

    private final CafeRepository cafeRepository;
    private final MenuItemRepository menuItemRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    @Autowired
    public ManagerController(CafeRepository cafeRepository,
                             MenuItemRepository menuItemRepository,
                             OrderRepository orderRepository,
                             UserRepository userRepository) {
        this.cafeRepository = cafeRepository;
        this.menuItemRepository = menuItemRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/cafes")
    public ResponseEntity<?> getManagerCafes(@AuthenticationPrincipal UserDetails userDetails) {
        String username = userDetails.getUsername();
        logger.info("Менеджер '{}' запитує список своїх кав'ярень", username);

        Optional<User> manager = userRepository.findByUsername(userDetails.getUsername());
        if (manager.isEmpty()) {
            logger.warn("Менеджера '{}' не знайдено", username);
            return ResponseEntity.status(403).body("Менеджера не знайдено");
        }

        List<Cafe> cafes = cafeRepository.findByManager(manager.get());
        logger.info("Знайдено {} кав'ярень для менеджера '{}'", cafes.size(), username);
        return ResponseEntity.ok(cafes);
    }

    @GetMapping("/cafes/{cafeId}/menu")
    public ResponseEntity<?> getCafeMenu(@PathVariable Long cafeId,
                                         @AuthenticationPrincipal UserDetails userDetails) {
        String username = userDetails.getUsername();
        logger.info("Менеджер '{}' запитує меню для кафе з ID={}", username, cafeId);

        Optional<User> manager = userRepository.findByUsername(userDetails.getUsername());
        if (manager.isEmpty()) {
            logger.warn("Менеджера '{}' не знайдено", userDetails.getUsername());
            return ResponseEntity.status(403).body("Менеджера не знайдено");
        }

        Optional<Cafe> cafeOpt = cafeRepository.findById(cafeId);
        if (cafeOpt.isEmpty() || !manager.get().equals(cafeOpt.get().getManager())) {
            logger.warn("Менеджер '{}' намагається отримати доступ до кафе ID={}, яке йому не належить", username, cafeId);
            return ResponseEntity.status(403).body("Ця кав'ярня вам не належить");
        }

        List<MenuItem> menuItems = menuItemRepository.findByCafeId(cafeId);
        logger.info("Знайдено {} позицій меню для кафе ID={}", menuItems.size(), cafeId);
        return ResponseEntity.ok(menuItems);
    }

    @GetMapping("/orders")
    public ResponseEntity<?> getOrdersFromManagedCafes(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) Long cafeId) {
        String username = userDetails.getUsername();
        logger.info("Менеджер '{}' запитує замовлення (фільтр кафе ID={})", username, cafeId);

        Optional<User> manager = userRepository.findByUsername(userDetails.getUsername());
        if (manager.isEmpty()) {
            logger.warn("Менеджера '{}' не знайдено", userDetails.getUsername());
            return ResponseEntity.status(403).body("Менеджера не знайдено");
        }

        List<Cafe> cafes = cafeRepository.findByManager(manager.get());
        List<Long> cafeIds = cafes.stream().map(Cafe::getId).collect(Collectors.toList());

        if (cafeId != null && !cafeIds.contains(cafeId)) {
            logger.warn("Менеджер '{}' намагається отримати замовлення кав'ярні, яка йому не належить (ID={})", userDetails.getUsername(), cafeId);
            return ResponseEntity.status(403).body("Ця кав'ярня вам не належить");
        }

        List<Order> orders = orderRepository.findAll().stream()
                .filter(order -> order.getCafe() != null &&
                        (cafeId == null ? cafeIds.contains(order.getCafe().getId()) : order.getCafe().getId().equals(cafeId)))
                .collect(Collectors.toList());

        logger.debug("Менеджер '{}' отримав {} замовлень", userDetails.getUsername(), orders.size());
        return ResponseEntity.ok(orders);
    }

    @PostMapping("/menu-items")
    public ResponseEntity<?> createMenuItem(@RequestBody MenuItem menuItem) {
        logger.info("Менеджер створює пункт меню: '{}', категорія: '{}', ціна: {}, кафе ID: {}",
                menuItem.getName(),
                menuItem.getCategory(),
                menuItem.getPrice(),
                menuItem.getCafe() != null ? menuItem.getCafe().getId() : null);

        if (menuItem.getName() == null || menuItem.getName().trim().isEmpty()) {
            logger.warn("Назва пункту меню відсутня або порожня");
            return ResponseEntity.badRequest().body("Назва є обов’язковою");
        }

        if (menuItem.getCategory() == null || menuItem.getCategory().trim().isEmpty()) {
            logger.warn("Категорія пункту меню відсутня або порожня");
            return ResponseEntity.badRequest().body("Категорія є обов’язковою");
        }

        if (menuItem.getPrice() == null || menuItem.getPrice() <= 0) {
            logger.warn("Некоректна ціна пункту меню: {}", menuItem.getPrice());
            return ResponseEntity.badRequest().body("Ціна має бути додатною та більшою за 0");
        }

        if (menuItem.getCafe() == null || menuItem.getCafe().getId() == null) {
            logger.warn("ID кав'ярні відсутній при створенні пункту меню");
            return ResponseEntity.badRequest().body("Кафе є обов’язковим");
        }

        Optional<Cafe> cafeOpt = cafeRepository.findById(menuItem.getCafe().getId());
        if (cafeOpt.isEmpty()) {
            logger.warn("Кафе з ID={} не знайдено при створенні пункту меню", menuItem.getCafe().getId());
            return ResponseEntity.badRequest().body("Кафе не знайдено");
        }

        menuItem.setCafe(cafeOpt.get());
        MenuItem saved = menuItemRepository.save(menuItem);
        logger.info("Пункт меню успішно створено: ID={}, Назва='{}'", saved.getId(), saved.getName());
        return ResponseEntity.ok(saved);
    }
}
