package com.dropspace;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity @Table(name = "users")
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    public String name;
    @Column(unique = true) public String email;
    @JsonIgnore public String passwordHash;
    @JsonIgnore public String token;
    public LocalDateTime createdAt = LocalDateTime.now();
}
