package com.dropspace;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
public class Product {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    public String name;
    public String category;   // pc, notebook, celular, console, acessorio
    public String brand;
    public BigDecimal price;  // preço à vista no Pix
    public BigDecimal oldPrice; // null quando não há promoção
    public int stock;
    public double rating;
    public int reviews;

    public Product() {}
    public Product(String name, String category, String brand, String price, String oldPrice, int stock, double rating, int reviews) {
        this.name = name; this.category = category; this.brand = brand;
        this.price = new BigDecimal(price);
        this.oldPrice = oldPrice == null ? null : new BigDecimal(oldPrice);
        this.stock = stock; this.rating = rating; this.reviews = reviews;
    }
}
