package com.example.cafe.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import com.example.cafe.model.User;
import com.example.cafe.repository.UserRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/auth/me")
    public ResponseEntity<?> getCurrentUser(Authentication authentication, HttpServletRequest request) {
        if (authentication == null || !authentication.isAuthenticated()) {
            logger.warn("Спроба доступу до /auth/me без авторизації");
            return ResponseEntity.status(401).body("Неавторизовано");
        }

        String username = authentication.getName();
        logger.info("Отримання інформації про поточного користувача: {}", username);

        Optional<User> optionalUser = userRepository.findByUsername(username);

        if (!optionalUser.isPresent()) {
            logger.error("Користувача '{}' не знайдено в базі", username);
            return ResponseEntity.status(404).body("Користувача не знайдено");
        }

        User user = optionalUser.get();
        logger.info("Користувач '{}' успішно знайдений. Ролі: {}", username, user.getRoles());

        Map<String, Object> response = new HashMap<>();
        response.put("username", user.getUsername());
        response.put("roles", user.getRoles());
        response.put("sessionId", request.getSession().getId());

        return ResponseEntity.ok(response);
    }
}

