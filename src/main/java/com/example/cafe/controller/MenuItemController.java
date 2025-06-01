package com.example.cafe.controller;

import com.example.cafe.service.MenuItemService;
import com.example.cafe.model.MenuItem;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@RestController
@RequestMapping("/menu")
@RequiredArgsConstructor
public class MenuItemController {

    private final MenuItemService menuItemService;

    @GetMapping
    public List<MenuItem> getAllMenuItems() {
        return menuItemService.getAllMenuItems();
    }

    @GetMapping("/{cafeId}")
    public List<MenuItem> getMenu(@PathVariable Long cafeId) {
        return menuItemService.getMenuByCafeId(cafeId);
    }

    @PostMapping
    public ResponseEntity<MenuItem> createMenuItem(@RequestBody MenuItem menuItem) {
        MenuItem savedMenuItem = menuItemService.addMenuItem(menuItem);
        return new ResponseEntity<>(savedMenuItem, HttpStatus.CREATED);
    }
}
