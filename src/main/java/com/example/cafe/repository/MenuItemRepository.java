package com.example.cafe.repository;

import com.example.cafe.model.MenuItem;
import com.example.cafe.model.Cafe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {
    List<MenuItem> findByCafeId(Long cafeId);
    List<MenuItem> findByCafe(Cafe cafe);
}
