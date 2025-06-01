package com.example.cafe.repository;

import com.example.cafe.model.Order;
import com.example.cafe.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByCustomerName(String customerName);
    List<Order> findByUser(User user);
    List<Order> findByUserId(Long userId);
}
