package com.example.cafe.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.ui.Model;

@Controller
public class PageController {

    private static final Logger logger = LoggerFactory.getLogger(PageController.class);

    @GetMapping("/me")
    public String mePage() {
        logger.info("Перехід на сторінку /me");
        return "me";
    }

    @GetMapping("/admin/dashboard")
    public String adminDashboard() {
        logger.info("Перехід на панель адміністратора /admin/dashboard");
        return "admin";
    }

    @GetMapping("/manager/dashboard")
    public String managerDashboard(Model model) {
        logger.info("Перехід на панель менеджера /manager/dashboard");
        return "manager";
    }

    @GetMapping("/customer/dashboard")
    public String customerDashboard() {
        logger.info("Перехід на панель клієнта /customer/dashboard");
        return "customer";
    }
}
