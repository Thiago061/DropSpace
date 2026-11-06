package com.dropspace;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
public class OrderItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    public Long productId;
    public String productName;
    public BigDecimal unitPrice;
    public int quantity;
}
