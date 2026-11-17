package com.dropspace;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity @Table(name = "users")
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    public String firstName;
    public String lastName;
    @JsonIgnore @Column(unique = true) public String cpf;        // somente dígitos
    @Column(unique = true) public String email;
    @JsonIgnore @Column(unique = true) public String username;   // usado apenas pelo administrador
    public String cep, street, district, city, uf;
    @JsonIgnore public String passwordHash;
    @JsonIgnore public String token;
    @JsonIgnore public LocalDateTime tokenExpires;
    public String role = "CLIENT"; // CLIENT ou ADMIN (nunca definido pelo cadastro público)
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_interests", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "category")
    public Set<String> interests = new LinkedHashSet<>(); // slugs das categorias de interesse
    public LocalDateTime createdAt = LocalDateTime.now();

    public String getName() { return lastName == null || lastName.isBlank() ? firstName : firstName + " " + lastName; }
}
