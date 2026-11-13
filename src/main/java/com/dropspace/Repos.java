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
<<<<<<< HEAD
interface Videos extends JpaRepository<Video, Long> {
    List<Video> findAllByOrderByPositionAsc();
}
=======
>>>>>>> c72c07fe08313c6df2b2e54c9bbf3e5ffeed1440
