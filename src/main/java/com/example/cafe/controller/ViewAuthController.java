package com.example.cafe.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ViewAuthController {

    private static final Logger logger = LoggerFactory.getLogger(ViewAuthController.class);

    @GetMapping("/auth/login")
    public String showLoginForm() {
        logger.info("Перехід на сторінку входу /auth/login");
        return "login";
    }

    @GetMapping("/auth/register")
    public String showRegisterForm() {
        logger.info("Перехід на сторінку реєстрації /auth/register");
        return "register";
    }
}
