package com.example.cafe.controller;

import com.example.cafe.service.CafeService;
import com.example.cafe.model.Cafe;
import com.example.cafe.model.MenuItem;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/cafes")
@RequiredArgsConstructor
public class CafeController {
    private final CafeService cafeService;

    @GetMapping
    public List<Cafe> getAllCafes() {
        return cafeService.getAllCafes();
    }

    @GetMapping("/{id}")
    public Cafe getCafeById(@PathVariable Long id) {
        return cafeService.getCafeById(id);
    }

    @PostMapping
    public Cafe createCafe(@RequestBody Cafe cafe) {
        return cafeService.createCafe(cafe);
    }

    @GetMapping("/{id}/menu")
    public List<MenuItem> getMenuItemsByCafe(@PathVariable Long id) {
        return cafeService.getMenuItemsByCafe(id);
    }
}