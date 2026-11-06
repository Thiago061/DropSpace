package com.dropspace;

import java.math.BigDecimal;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
public class ApiController {
    record Register(String name, String email, String password) {}
    record Login(String email, String password) {}
    record Item(Long productId, int quantity) {}
    record Checkout(List<Item> items) {}

    private final Users users; private final Products products; private final Orders orders;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    ApiController(Users u, Products p, Orders o) { users = u; products = p; orders = o; }

    // ---------- Produtos ----------
    @GetMapping("/products")
    public List<Product> list(@RequestParam(required = false) String q, @RequestParam(required = false) String category) {
        String term = q == null ? "" : q.toLowerCase().trim();
        return products.findAll().stream()
            .filter(p -> category == null || category.isBlank() || p.category.equals(category))
            .filter(p -> term.isEmpty() || p.name.toLowerCase().contains(term) || p.brand.toLowerCase().contains(term))
            .toList();
    }

    @GetMapping("/products/{id}")
    public Product one(@PathVariable Long id) {
        return products.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Produto não encontrado"));
    }

    // ---------- Conta ----------
    @PostMapping("/auth/register")
    public Map<String, Object> register(@RequestBody Register r) {
        if (r.name() == null || r.name().isBlank() || r.email() == null || !r.email().contains("@"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe nome e e-mail válidos");
        if (r.password() == null || r.password().length() < 6)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A senha precisa ter ao menos 6 caracteres");
        if (users.findByEmail(r.email().toLowerCase()).isPresent())
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Este e-mail já está cadastrado");
        User u = new User();
        u.name = r.name().trim(); u.email = r.email().toLowerCase().trim();
        u.passwordHash = encoder.encode(r.password());
        return session(u);
    }

    @PostMapping("/auth/login")
    public Map<String, Object> login(@RequestBody Login l) {
        User u = users.findByEmail(l.email() == null ? "" : l.email().toLowerCase().trim()).orElse(null);
        if (u == null || !encoder.matches(l.password() == null ? "" : l.password(), u.passwordHash))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "E-mail ou senha incorretos");
        return session(u);
    }

    @GetMapping("/me")
    public User me(@RequestHeader(value = "Authorization", required = false) String h) { return auth(h); }

    // ---------- Pedidos ----------
    @PostMapping("/orders")
    @Transactional
    public CustomerOrder checkout(@RequestHeader(value = "Authorization", required = false) String h, @RequestBody Checkout c) {
        User u = auth(h);
        if (c.items() == null || c.items().isEmpty())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Seu carrinho está vazio");
        CustomerOrder o = new CustomerOrder();
        o.userId = u.id; o.total = BigDecimal.ZERO;
        for (Item i : c.items()) {
            Product p = products.findById(i.productId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Produto não encontrado"));
            if (i.quantity() < 1 || i.quantity() > p.stock)
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Estoque insuficiente para: " + p.name);
            p.stock -= i.quantity();
            OrderItem oi = new OrderItem();
            oi.productId = p.id; oi.productName = p.name; oi.unitPrice = p.price; oi.quantity = i.quantity();
            o.items.add(oi);
            o.total = o.total.add(p.price.multiply(BigDecimal.valueOf(i.quantity())));
        }
        return orders.save(o);
    }

    @GetMapping("/orders")
    public List<CustomerOrder> myOrders(@RequestHeader(value = "Authorization", required = false) String h) {
        return orders.findByUserIdOrderByCreatedAtDesc(auth(h).id);
    }

    // ---------- Auxiliares ----------
    private Map<String, Object> session(User u) {
        u.token = UUID.randomUUID().toString();
        users.save(u);
        return Map.of("token", u.token, "name", u.name);
    }

    private User auth(String header) {
        if (header == null || !header.startsWith("Bearer "))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Faça login para continuar");
        return users.findByToken(header.substring(7))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sessão expirada. Entre novamente"));
    }
}
