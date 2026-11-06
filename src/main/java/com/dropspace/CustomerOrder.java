package com.dropspace;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity @Table(name = "orders")
public class CustomerOrder {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    public Long userId;
    public BigDecimal total;
    public String status = "PAGAMENTO_PENDENTE";
    public LocalDateTime createdAt = LocalDateTime.now();
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "order_id")
    public List<OrderItem> items = new ArrayList<>();
}
