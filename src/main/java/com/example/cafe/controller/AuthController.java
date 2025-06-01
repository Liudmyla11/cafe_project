package com.example.cafe.controller;

import com.example.cafe.model.User;
import com.example.cafe.repository.UserRepository;
import com.example.cafe.util.JwtUtil;
import com.example.cafe.service.UserDetailsServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.userdetails.UserDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/auth/api")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @PostMapping("/register")
    public ResponseEntity<?> registerUserApi(@RequestBody Map<String, String> userMap) {
        String username = userMap.get("username");
        String password = userMap.get("password");
        String role = userMap.getOrDefault("role", "ROLE_CUSTOMER");

        logger.info("Спроба реєстрації користувача: {}", username);

        if (username == null || password == null) {
            logger.warn("Не вказано ім'я користувача або пароль");
            return ResponseEntity.badRequest().body("Потрібні ім'я користувача та пароль");
        }

        if (userRepository.existsByUsername(username)) {
            logger.warn("Ім'я користувача '{}' вже зайняте", username);
            return ResponseEntity.badRequest().body("Ім'я користувача вже зайняте");
        }

        if (!isValidRole(role)) {
            logger.warn("Невалідна роль '{}'. Використовується роль за замовчуванням", role);
            role = "ROLE_CUSTOMER";
        }

        User user = new User(username, passwordEncoder.encode(password), Collections.singleton(role));
        userRepository.save(user);

        logger.info("Користувач '{}' успішно зареєстрований з роллю '{}'", username, role);
        return ResponseEntity.ok("Користувач успішно зареєструвався");
    }

    @PostMapping("/login")
    public ResponseEntity<?> loginUserApi(@RequestBody Map<String, String> loginMap, HttpServletRequest request, HttpServletResponse response) {
        String username = loginMap.get("username");
        String password = loginMap.get("password");

        logger.info("Спроба входу користувача: {}", username);

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password)
            );

            HttpSession oldSession = request.getSession(false);
            if (oldSession != null) {
                oldSession.invalidate();
            }
            request.getSession(true);

            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            String token = jwtUtil.generateToken(userDetails.getUsername(), userDetails.getAuthorities());

            Cookie cookie = new Cookie("jwt", token);
            cookie.setHttpOnly(true);
            cookie.setPath("/");
            cookie.setMaxAge(60 * 60 * 24);
            response.addCookie(cookie);

            logger.info("Користувач '{}' успішно увійшов у систему", username);
            return ResponseEntity.ok(Collections.singletonMap("token", token));
        } catch (AuthenticationException e) {
            logger.warn("Невдала спроба входу для користувача '{}'", username);
            return ResponseEntity.status(401).body("Недійсне ім'я користувача або пароль");
        }
    }

    private boolean isValidRole(String role) {
        return role.equals("ROLE_ADMIN") || role.equals("ROLE_MANAGER") || role.equals("ROLE_CUSTOMER");
    }

    @PostMapping("/auth/logout")
    public void logout(HttpServletResponse response) {
        Cookie cookie = new Cookie("jwt", null);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
        logger.info("Користувач вийшов із системи (JWT cookie видалено)");
    }
}
