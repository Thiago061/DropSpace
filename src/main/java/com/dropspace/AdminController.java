package com.dropspace;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/** Estoque e catálogo. Todas as rotas exigem perfil ADMIN (verificado no servidor). */
@RestController
@RequestMapping("/api/admin")
public class AdminController {
    record ProductForm(String name, String category, String brand, String sku, BigDecimal price, BigDecimal oldPrice, Integer stock) {}
    record StockForm(String type, int quantity, String note) {}
    record CategoryForm(String name, String icon) {}

    private final Products products; private final StockMovements movements; private final AuthService auth;
    private final Categories categories; private final Users users; private final Orders orders;
    AdminController(Products p, StockMovements m, AuthService a, Categories c, Users u, Orders o) {
        products = p; movements = m; auth = a; categories = c; users = u; orders = o;
    }

    // ---------- Visão geral ----------
    @GetMapping("/summary")
    public Map<String, Object> summary(@RequestHeader(value = "Authorization", required = false) String h) {
        auth.admin(h);
        List<Product> all = products.findAll();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("products", all.size());
        m.put("categories", categories.count());
        m.put("units", all.stream().mapToInt(p -> p.stock).sum());
        m.put("lowStock", all.stream().filter(p -> p.stock <= 3).count());
        m.put("stockValue", all.stream().map(p -> p.price.multiply(BigDecimal.valueOf(p.stock))).reduce(BigDecimal.ZERO, BigDecimal::add));
        m.put("customers", users.findAll().stream().filter(u -> "CLIENT".equals(u.role)).count());
        m.put("orders", orders.count());
        m.put("revenue", orders.findAll().stream().map(o -> o.total).reduce(BigDecimal.ZERO, BigDecimal::add));
        return m;
    }

    // ---------- Categorias ----------
    @PostMapping("/categories")
    public Category createCategory(@RequestHeader(value = "Authorization", required = false) String h, @RequestBody CategoryForm f) {
        auth.admin(h);
        String name = validName(f);
        String slug = Normalizer.normalize(name, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
        if (slug.isEmpty()) throw bad("Nome de categoria inválido");
        if (categories.existsById(slug)) throw new ResponseStatusException(HttpStatus.CONFLICT, "Esta categoria já existe");
        return categories.save(new Category(slug, name, icon(f), (int) categories.count() + 1));
    }

    @PutMapping("/categories/{slug}")
    public Category updateCategory(@RequestHeader(value = "Authorization", required = false) String h, @PathVariable String slug, @RequestBody CategoryForm f) {
        auth.admin(h);
        Category c = categories.findById(slug).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Categoria não encontrada"));
        c.name = validName(f); c.icon = icon(f);
        return categories.save(c);
    }

    @DeleteMapping("/categories/{slug}")
    public void deleteCategory(@RequestHeader(value = "Authorization", required = false) String h, @PathVariable String slug) {
        auth.admin(h);
        if (products.existsByCategory(slug)) throw new ResponseStatusException(HttpStatus.CONFLICT, "Existem produtos nesta categoria. Mova ou exclua os produtos antes");
        categories.deleteById(slug);
    }

    @GetMapping("/products")
    public List<Product> all(@RequestHeader(value = "Authorization", required = false) String h) {
        auth.admin(h);
        return products.findAll();
    }

    @PostMapping("/products")
    @Transactional
    public Product create(@RequestHeader(value = "Authorization", required = false) String h, @RequestBody ProductForm f) {
        User u = auth.admin(h);
        Product p = new Product();
        apply(p, f);
        int initial = f.stock() == null ? 0 : f.stock();
        if (initial < 0) throw bad("O estoque inicial não pode ser negativo");
        p.stock = initial;
        p = products.save(p);
        if (initial > 0) movements.save(StockMovement.of(p, "ENTRADA", initial, "Estoque inicial", u.id));
        return p;
    }

    @PutMapping("/products/{id}")
    @Transactional
    public Product update(@RequestHeader(value = "Authorization", required = false) String h, @PathVariable Long id, @RequestBody ProductForm f) {
        auth.admin(h);
        Product p = find(id);
        apply(p, f); // o estoque só muda por movimentação, nunca por edição
        return products.save(p);
    }

    @DeleteMapping("/products/{id}")
    @Transactional
    public void delete(@RequestHeader(value = "Authorization", required = false) String h, @PathVariable Long id) throws IOException {
        auth.admin(h);
        Product p = find(id);
        if (p.imageUrl != null && p.imageUrl.startsWith("/uploads/"))
            Files.deleteIfExists(Path.of("uploads").toAbsolutePath().resolve(Path.of(p.imageUrl).getFileName()));
        products.delete(p); // pedidos antigos guardam nome e preço próprios, então o histórico é preservado
    }

    @PostMapping("/products/{id}/stock")
    @Transactional
    public Product stock(@RequestHeader(value = "Authorization", required = false) String h, @PathVariable Long id, @RequestBody StockForm f) {
        User u = auth.admin(h);
        Product p = find(id);
        int q = f.quantity();
        String type = f.type() == null ? "" : f.type();
        int delta = switch (type) {
            case "ENTRADA" -> { if (q <= 0) throw bad("Informe uma quantidade maior que zero"); yield q; }
            case "SAIDA" -> { if (q <= 0) throw bad("Informe uma quantidade maior que zero"); yield -q; }
            case "AJUSTE" -> { if (q < 0) throw bad("A contagem não pode ser negativa"); yield q - p.stock; }
            default -> throw bad("Tipo de movimentação inválido");
        };
        if (p.stock + delta < 0) throw new ResponseStatusException(HttpStatus.CONFLICT, "O estoque não pode ficar negativo");
        p.stock += delta;
        p = products.save(p);
        movements.save(StockMovement.of(p, type, delta, f.note(), u.id));
        return p;
    }

    @PostMapping("/products/{id}/image")
    public Product image(@RequestHeader(value = "Authorization", required = false) String h, @PathVariable Long id,
                         @RequestParam("file") MultipartFile file) throws IOException {
        auth.admin(h);
        Product p = find(id);
        String ext = switch (String.valueOf(file.getContentType())) {
            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            default -> throw bad("Envie uma imagem JPG, PNG ou WEBP");
        };
        if (file.isEmpty() || file.getSize() > 2 * 1024 * 1024) throw bad("A imagem deve ter até 2 MB");
        Path dir = Path.of("uploads").toAbsolutePath();
        Files.createDirectories(dir);
        String name = UUID.randomUUID() + "." + ext; // nome gerado pelo servidor: ignora o nome enviado
        file.transferTo(dir.resolve(name));
        p.imageUrl = "/uploads/" + name;
        return products.save(p);
    }

    @GetMapping("/movements")
    public List<StockMovement> history(@RequestHeader(value = "Authorization", required = false) String h) {
        auth.admin(h);
        return movements.findTop50ByOrderByCreatedAtDesc();
    }

    private void apply(Product p, ProductForm f) {
        if (f.name() == null || f.name().isBlank()) throw bad("Informe o nome do produto");
        if (f.category() == null || !categories.existsById(f.category())) throw bad("Categoria inválida");
        if (f.price() == null || f.price().signum() <= 0) throw bad("Informe um preço maior que zero");
        if (f.oldPrice() != null && f.oldPrice().compareTo(f.price()) <= 0) throw bad("O preço antigo precisa ser maior que o preço atual (ou deixe em branco)");
        p.name = f.name().trim(); p.category = f.category();
        p.brand = f.brand() == null || f.brand().isBlank() ? "Sem marca" : f.brand().trim();
        p.sku = f.sku() == null || f.sku().isBlank() ? null : f.sku().trim();
        p.price = f.price(); p.oldPrice = f.oldPrice();
    }

    private static String validName(CategoryForm f) {
        if (f.name() == null || f.name().trim().length() < 2 || f.name().trim().length() > 40) throw bad("Informe o nome da categoria (2 a 40 letras)");
        return f.name().trim();
    }
    private static String icon(CategoryForm f) { return f.icon() == null || f.icon().isBlank() ? "📦" : f.icon().trim(); }

    private Product find(Long id) {
        return products.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Produto não encontrado"));
    }
    private static ResponseStatusException bad(String m) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, m); }
}
