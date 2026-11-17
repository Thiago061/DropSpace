package com.dropspace;

import java.math.BigDecimal;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
public class ApiController {
    record Item(Long productId, int quantity) {}
    record Checkout(List<Item> items, String cep) {}

    private final Products products; private final Orders orders;
    private final StockMovements movements; private final AuthService auth; private final ShippingService shipping;
    private final Categories categories;

    ApiController(Products p, Orders o, StockMovements m, AuthService a, ShippingService s, Categories c) {
        categories = c; products = p; orders = o; movements = m; auth = a; shipping = s;
    }

    // ---------- Produtos ----------
    @GetMapping("/products")
    public List<Product> list(@RequestParam(required = false) String q, @RequestParam(required = false) String category) {
        String term = q == null ? "" : q.toLowerCase().trim();
        return products.findAll().stream()
            .filter(p -> category == null || category.isBlank() || p.category.equals(category))
            .filter(p -> term.isEmpty() || p.name.toLowerCase().contains(term) || (p.brand != null && p.brand.toLowerCase().contains(term)))
            .toList();
    }

    @GetMapping("/products/{id}")
    public Product one(@PathVariable Long id) {
        return products.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Produto não encontrado"));
    }

    // ---------- Frete ----------
    @GetMapping("/shipping")
    public ShippingService.Quote quote(@RequestParam String cep, @RequestParam(defaultValue = "0") BigDecimal subtotal) {
        return shipping.quote(cep, subtotal);
    }

    @GetMapping("/address/{cep}")
    public ShippingService.Address address(@PathVariable String cep) { return shipping.lookup(cep); }

    @GetMapping("/categories")
    public List<Category> categories() { return categories.findAllByOrderByPositionAsc(); }

    // ---------- Pedidos ----------
    @PostMapping("/orders")
    @Transactional
    public CustomerOrder checkout(@RequestHeader(value = "Authorization", required = false) String h, @RequestBody Checkout c) {
        User u = auth.user(h);
        if (c.items() == null || c.items().isEmpty())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Seu carrinho está vazio");
        CustomerOrder o = new CustomerOrder();
        o.userId = u.id; o.subtotal = BigDecimal.ZERO;
        List<StockMovement> logs = new ArrayList<>();
        for (Item i : c.items()) {
            Product p = products.findById(i.productId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Produto não encontrado"));
            if (i.quantity() < 1 || i.quantity() > p.stock)
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Estoque insuficiente para: " + p.name);
            p.stock -= i.quantity();
            OrderItem oi = new OrderItem();
            oi.productId = p.id; oi.productName = p.name; oi.unitPrice = p.price; oi.quantity = i.quantity();
            o.items.add(oi);
            o.subtotal = o.subtotal.add(p.price.multiply(BigDecimal.valueOf(i.quantity())));
            logs.add(StockMovement.of(p, "VENDA", -i.quantity(), "Venda online", u.id));
        }
        ShippingService.Quote q = shipping.quote(c.cep() == null || c.cep().isBlank() ? u.cep : c.cep(), o.subtotal); // o frete é sempre calculado no servidor
        o.cep = q.cep(); o.shipping = q.price(); o.total = o.subtotal.add(q.price());
        CustomerOrder saved = orders.save(o);
        movements.saveAll(logs);
        return saved;
    }

    @GetMapping("/orders")
    public List<CustomerOrder> myOrders(@RequestHeader(value = "Authorization", required = false) String h) {
        return orders.findByUserIdOrderByCreatedAtDesc(auth.user(h).id);
    }
}
