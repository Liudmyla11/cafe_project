package com.example.cafe.service;

import com.example.cafe.repository.MenuItemRepository;
import com.example.cafe.model.MenuItem;
import com.example.cafe.model.Cafe;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MenuItemService {

    private final MenuItemRepository menuItemRepository;
    private final CafeService cafeService;

    public MenuItem getById(Long id) {
        return menuItemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("MenuItem not found"));
    }

    public List<MenuItem> getMenuByCafeId(Long cafeId) {
        return menuItemRepository.findByCafeId(cafeId);
    }

    public List<MenuItem> getAllMenuItems() {
        return menuItemRepository.findAll();
    }

    public MenuItem addMenuItem(MenuItem menuItem) {
        if (menuItem.getCafe() != null && menuItem.getCafe().getId() != null) {
            Long cafeId = menuItem.getCafe().getId();
            Cafe cafe = cafeService.getCafeById(cafeId);
            menuItem.setCafe(cafe);
        }
        return menuItemRepository.save(menuItem);
    }
}
