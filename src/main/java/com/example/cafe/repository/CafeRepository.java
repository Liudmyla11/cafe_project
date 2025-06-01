package com.example.cafe.repository;

import com.example.cafe.model.Cafe;
import com.example.cafe.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CafeRepository extends JpaRepository<Cafe, Long> {
    Optional<Cafe> findByName(String name);
    boolean existsByManager(User manager);
    List<Cafe> findByManager(User manager);
}
