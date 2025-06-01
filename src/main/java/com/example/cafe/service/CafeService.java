package com.example.cafe.service;

import com.example.cafe.repository.CafeRepository;
import com.example.cafe.repository.MenuItemRepository;
import com.example.cafe.model.Cafe;
import com.example.cafe.model.MenuItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CafeService {

    private final CafeRepository cafeRepository;
    private final MenuItemRepository menuItemRepository;

    public List<Cafe> getAllCafes() {
        return cafeRepository.findAll();
    }

    public Cafe createCafe(Cafe cafe) {
        return cafeRepository.save(cafe);
    }

    public Cafe getCafeById(Long id) {
        return cafeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cafe not found"));
    }

    public List<MenuItem> getMenuItemsByCafe(Long cafeId) {
        if (!cafeRepository.existsById(cafeId)) {
            throw new RuntimeException("Cafe not found");
        }
        return menuItemRepository.findByCafeId(cafeId);
    }
}
