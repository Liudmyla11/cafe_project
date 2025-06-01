package com.example.cafe.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.HttpStatus;
import com.example.cafe.model.Cafe;
import com.example.cafe.model.Order;
import com.example.cafe.model.User;
import com.example.cafe.model.MenuItem;
import com.example.cafe.repository.CafeRepository;
import com.example.cafe.repository.MenuItemRepository;
import com.example.cafe.repository.OrderRepository;
import com.example.cafe.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

    private final UserRepository userRepository;
    private final CafeRepository cafeRepository;
    private final OrderRepository orderRepository;
    private final MenuItemRepository menuItemRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    public AdminController(UserRepository userRepository,
                           CafeRepository cafeRepository,
                           OrderRepository orderRepository, MenuItemRepository menuItemRepository) {
        this.userRepository = userRepository;
        this.cafeRepository = cafeRepository;
        this.orderRepository = orderRepository;
        this.menuItemRepository = menuItemRepository;
    }

    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        logger.info("Отримання списку всіх користувачів");
        List<User> users = userRepository.findAll();
        logger.debug("Знайдено {} користувачів", users.size());
        return ResponseEntity.ok(users);
    }

    @GetMapping("/cafes")
    public ResponseEntity<List<Cafe>> getAllCafes() {
        logger.info("Отримання списку всіх кафе");
        List<Cafe> cafes = cafeRepository.findAll();
        logger.debug("Знайдено {} кафе", cafes.size());
        return ResponseEntity.ok(cafes);
    }

    @GetMapping("/orders")
    public ResponseEntity<List<Order>> getAllOrders() {
        logger.info("Отримання списку всіх замовлень");
        List<Order> orders = orderRepository.findAll();
        logger.debug("Знайдено {} замовлень", orders.size());
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/cafes/{id}/menu")
    public ResponseEntity<List<MenuItem>> getMenuItemsByCafe(@PathVariable Long id) {
        logger.info("Отримання меню для кафе id={}", id);
        if (!cafeRepository.existsById(id)) {
            logger.warn("Кафе з id={} не знайдено", id);
            return ResponseEntity.notFound().build();
        }
        try {
            List<MenuItem> items = menuItemRepository.findByCafeId(id);
            logger.debug("Знайдено {} пунктів меню для кафе id={}", items.size(), id);
            return ResponseEntity.ok(items);
        } catch (Exception e) {
            logger.error("Помилка при отриманні меню для кафе id={}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/users")
    public ResponseEntity<?> createUser(@RequestBody User user) {
        logger.info("Спроба створення користувача з username={}", user.getUsername());

        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            logger.warn("Користувач з username={} вже існує", user.getUsername());
            return ResponseEntity.badRequest().body("Ім'я користувача вже існує");
        }

        user.setId(null);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setEnabled(true);

        if (user.getRoles() == null || user.getRoles().isEmpty()) {
            user.setRoles(Collections.singleton("ROLE_CUSTOMER"));
        }

        User savedUser = userRepository.save(user);
        logger.info("Користувач створений успішно з id={}", savedUser.getId());

        return ResponseEntity.ok(savedUser);
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody User updatedUser) {
        logger.info("Спроба оновлення користувача з id={}", id);

        return userRepository.findById(id)
                .map(user -> {
                    if (updatedUser.getUsername() != null) {
                        Optional<User> userWithSameUsername = userRepository.findByUsername(updatedUser.getUsername());
                        if (userWithSameUsername.isPresent() && !userWithSameUsername.get().getId().equals(id)) {
                            logger.warn("Користувач з username={} вже існує", updatedUser.getUsername());
                            return ResponseEntity.status(HttpStatus.CONFLICT)
                                    .body("Користувач з таким іменем вже існує");
                        }
                        logger.debug("Оновлення username користувача id={} на {}", id, updatedUser.getUsername());
                        user.setUsername(updatedUser.getUsername());
                    }

                    if (updatedUser.getPassword() != null) {
                        logger.debug("Оновлення пароля користувача id={}", id);
                        user.setPassword(passwordEncoder.encode(updatedUser.getPassword()));
                    }

                    if (updatedUser.getRoles() != null) {
                        logger.debug("Оновлення ролей користувача id={} на {}", id, updatedUser.getRoles());
                        user.setRoles(updatedUser.getRoles());
                    }

                    if (updatedUser.getEnabled() != null) {
                        logger.debug("Оновлення статусу enabled користувача id={} на {}", id, updatedUser.getEnabled());
                        user.setEnabled(updatedUser.getEnabled());
                    }

                    userRepository.save(user);
                    logger.info("Користувач id={} оновлений успішно", id);
                    return ResponseEntity.ok(user);
                })
                .orElseGet(() -> {
                    logger.warn("Користувача з id={} не знайдено", id);
                    return ResponseEntity.notFound().build();
                });
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        logger.info("Спроба видалення користувача з id={}", id);

        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            logger.warn("Користувача з id={} не знайдено", id);
            return ResponseEntity.notFound().build();
        }

        User user = userOpt.get();

        if (user.getRoles().contains("ROLE_MANAGER") && cafeRepository.existsByManager(user)) {
            logger.warn("Видалення менеджера id={} неможливе - прив'язане кафе", id);
            return ResponseEntity.badRequest()
                    .body("Менеджера видалити неможливо, адже за ним закріплене кафе");
        }

        userRepository.deleteById(id);
        logger.info("Користувач id={} успішно видалений", id);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/cafes")
    public ResponseEntity<?> createCafe(@RequestBody Cafe cafe) {
        logger.info("Спроба створення кафе: {}", cafe);

        if (cafe.getName() == null || cafe.getName().trim().isEmpty()) {
            logger.warn("Не вказана назва кафе");
            return ResponseEntity.badRequest().body("Назва кафе є обов'язковою");
        }

        if (cafe.getCity() == null || cafe.getCity().trim().isEmpty()) {
            logger.warn("Не вказано місто кафе");
            return ResponseEntity.badRequest().body("Місто є обов'язковим");
        }

        if (cafe.getManager() != null) {
            Long managerId = cafe.getManager().getId();
            if (managerId == null) {
                logger.warn("ID менеджера не вказано");
                return ResponseEntity.badRequest().body("ID менеджера не вказано");
            }

            Optional<User> optionalManager = userRepository.findById(managerId);
            if (optionalManager.isEmpty()) {
                logger.warn("Менеджера з id={} не знайдено", managerId);
                return ResponseEntity.badRequest().body("Менеджера не знайдено");
            }

            User manager = optionalManager.get();

            if (manager.getRoles() == null || !manager.getRoles().contains("ROLE_MANAGER")) {
                logger.warn("Користувач id={} не є менеджером", managerId);
                return ResponseEntity.badRequest().body("Вказаний користувач не є менеджером");
            }

            cafe.setManager(manager);
        }

        Cafe saved = cafeRepository.save(cafe);
        logger.info("Кафе створено успішно з id={}", saved.getId());

        return ResponseEntity.ok(saved);
    }

    @PutMapping("/cafes/{id}")
    public ResponseEntity<?> updateCafe(@PathVariable Long id, @RequestBody Cafe updatedCafe) {
        logger.info("Спроба оновлення кафе з id={}", id);

        return cafeRepository.findById(id)
                .map(cafe -> {
                    if (updatedCafe.getName() != null) {
                        logger.debug("Оновлення назви кафе id={} на {}", id, updatedCafe.getName());
                        cafe.setName(updatedCafe.getName());
                    }

                    if (updatedCafe.getAddress() != null) {
                        logger.debug("Оновлення адреси кафе id={} на {}", id, updatedCafe.getAddress());
                        cafe.setAddress(updatedCafe.getAddress());
                    }

                    if (updatedCafe.getCity() != null) {
                        logger.debug("Оновлення міста кафе id={} на {}", id, updatedCafe.getCity());
                        cafe.setCity(updatedCafe.getCity());
                    }

                    if (updatedCafe.getManager() != null) {
                        logger.debug("Оновлення менеджера кафе id={} на {}", id, updatedCafe.getManager());
                        cafe.setManager(updatedCafe.getManager());
                    }

                    cafeRepository.save(cafe);
                    logger.info("Кафе id={} оновлено успішно", id);
                    return ResponseEntity.ok(cafe);
                })
                .orElseGet(() -> {
                    logger.warn("Кафе з id={} не знайдено", id);
                    return ResponseEntity.notFound().build();
                });
    }

    @DeleteMapping("/cafes/{id}")
    public ResponseEntity<?> deleteCafe(@PathVariable Long id) {
        logger.info("Спроба видалення кафе з id={}", id);

        if (!cafeRepository.existsById(id)) {
            logger.warn("Кафе з id={} не знайдено", id);
            return ResponseEntity.notFound().build();
        }

        cafeRepository.deleteById(id);
        logger.info("Кафе з id={} успішно видалено", id);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/menu-items")
    public ResponseEntity<?> createMenuItem(@RequestBody MenuItem menuItem) {
        logger.info("Спроба створення пункту меню: {}", menuItem);

        if (menuItem.getCafe() == null) {
            logger.warn("Cafe у пункті меню не вказано");
        } else {
            logger.debug("Cafe id у пункті меню: {}", menuItem.getCafe().getId());
        }

        if (menuItem.getName() == null || menuItem.getName().trim().isEmpty()) {
            logger.warn("Назва пункту меню не вказана");
            return ResponseEntity.badRequest().body("Назва є обов’язковою");
        }

        if (menuItem.getCategory() == null || menuItem.getCategory().trim().isEmpty()) {
            logger.warn("Категорія пункту меню не вказана");
            return ResponseEntity.badRequest().body("Категорія є обов’язковою");
        }

        if (menuItem.getPrice() == null || menuItem.getPrice() <= 0) {
            logger.warn("Неправильна ціна пункту меню: {}", menuItem.getPrice());
            return ResponseEntity.badRequest().body("Ціна має бути додатною та більшою за 0");
        }

        if (menuItem.getCafe() == null || menuItem.getCafe().getId() == null) {
            logger.warn("Кафе у пункті меню не вказано або відсутній id");
            return ResponseEntity.badRequest().body("Кафе є обов’язковим");
        }

        Optional<Cafe> cafeOpt = cafeRepository.findById(menuItem.getCafe().getId());
        if (cafeOpt.isEmpty()) {
            logger.warn("Кафе з id={} не знайдено", menuItem.getCafe().getId());
            return ResponseEntity.badRequest().body("Кафе не знайдено");
        }

        menuItem.setCafe(cafeOpt.get());

        MenuItem saved = menuItemRepository.save(menuItem);
        logger.info("Пункт меню створено успішно з id={}", saved.getId());

        return ResponseEntity.ok(saved);
    }
}