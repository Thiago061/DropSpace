package com.dropspace;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface Users extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByUsername(String username);
    Optional<User> findByCpf(String cpf);
    Optional<User> findByToken(String token);
}
interface Products extends JpaRepository<Product, Long> {
    boolean existsByCategory(String category);
}
interface Categories extends JpaRepository<Category, String> {
    List<Category> findAllByOrderByPositionAsc();
}
interface Orders extends JpaRepository<CustomerOrder, Long> {
    List<CustomerOrder> findByUserIdOrderByCreatedAtDesc(Long userId);
}
interface Videos extends JpaRepository<Video, Long> {
    List<Video> findAllByOrderByPositionAsc();
}
interface StockMovements extends JpaRepository<StockMovement, Long> {
    List<StockMovement> findTop50ByOrderByCreatedAtDesc();
}
