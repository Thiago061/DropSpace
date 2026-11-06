package com.dropspace;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface Users extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByToken(String token);
}
interface Products extends JpaRepository<Product, Long> {}
interface Orders extends JpaRepository<CustomerOrder, Long> {
    List<CustomerOrder> findByUserIdOrderByCreatedAtDesc(Long userId);
}
