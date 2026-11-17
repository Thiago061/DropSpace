package com.dropspace;

import java.time.LocalDateTime;
import java.util.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/** Cadastro, login, perfil e interesses do cliente. */
@RestController
@RequestMapping("/api")
public class AuthController {
    record Register(String firstName, String lastName, String cpf, String cep, String email, String password) {}
    record Login(String login, String password) {}
    record Interests(List<String> categories) {}

    private static final String EMAIL = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$";
    private final Users users; private final Categories categories; private final AuthService auth;
    private final ShippingService shipping; private final BCryptPasswordEncoder encoder;

    AuthController(Users u, Categories c, AuthService a, ShippingService s, BCryptPasswordEncoder e) {
        users = u; categories = c; auth = a; shipping = s; encoder = e;
    }

    @PostMapping("/auth/register")
    public Map<String, Object> register(@RequestBody Register r) {
        String first = clean(r.firstName()), last = clean(r.lastName()), email = clean(r.email()).toLowerCase();
        if (first.length() < 2 || last.length() < 2) throw bad("Informe nome e sobrenome");
        if (!Cpf.isValid(r.cpf())) throw bad("CPF inválido");
        if (!email.matches(EMAIL)) throw bad("E-mail inválido");
        String pw = r.password();
        if (pw == null || pw.length() < 6 || pw.length() > 8) throw bad("A senha deve ter de 6 a 8 caracteres");
        String cpf = Cpf.normalize(r.cpf());
        if (users.findByEmail(email).isPresent()) throw new ResponseStatusException(HttpStatus.CONFLICT, "Este e-mail já está cadastrado");
        if (users.findByCpf(cpf).isPresent()) throw new ResponseStatusException(HttpStatus.CONFLICT, "Este CPF já está cadastrado");

        User u = new User(); // o perfil é sempre CLIENT: ninguém vira administrador pelo cadastro
        u.firstName = first; u.lastName = last; u.cpf = cpf; u.email = email;
        u.passwordHash = encoder.encode(pw);
        try {
            ShippingService.Address a = shipping.lookup(r.cep());
            u.cep = a.cep(); u.street = a.street(); u.district = a.district(); u.city = a.city(); u.uf = a.uf();
        } catch (ResponseStatusException e) {
            if (e.getStatusCode().value() != 502) throw e;   // CEP inválido/inexistente bloqueia; falha do ViaCEP não
            u.cep = r.cep().replaceAll("\\D", "");
        }
        try { return session(u); }
        catch (DataIntegrityViolationException e) { throw new ResponseStatusException(HttpStatus.CONFLICT, "E-mail ou CPF já cadastrado"); }
    }

    @PostMapping("/auth/login")
    public Map<String, Object> login(@RequestBody Login l) {
        String login = l.login() == null ? "" : l.login().toLowerCase().trim();
        auth.guard(login);
        User u = users.findByEmail(login).or(() -> users.findByUsername(login)).orElse(null);
        if (u == null || l.password() == null || !encoder.matches(l.password(), u.passwordHash)) {
            auth.fail(login);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário/e-mail ou senha incorretos");
        }
        auth.clear(login);
        return session(u);
    }

    @PostMapping("/auth/logout")
    public void logout(@RequestHeader(value = "Authorization", required = false) String h) {
        User u = auth.user(h); u.token = null; u.tokenExpires = null; users.save(u);
    }

    @GetMapping("/me")
    public User me(@RequestHeader(value = "Authorization", required = false) String h) { return auth.user(h); }

    @PutMapping("/me/interests")
    public Set<String> interests(@RequestHeader(value = "Authorization", required = false) String h, @RequestBody Interests in) {
        User u = auth.user(h);
        List<String> wanted = in.categories() == null ? List.of() : in.categories();
        Set<String> valid = new HashSet<>();
        categories.findAll().forEach(c -> valid.add(c.slug));
        if (!valid.containsAll(wanted)) throw bad("Categoria de interesse inválida");
        u.interests = new LinkedHashSet<>(wanted);
        return users.save(u).interests;
    }

    private Map<String, Object> session(User u) {
        u.token = UUID.randomUUID().toString();
        u.tokenExpires = LocalDateTime.now().plusHours(8);
        users.save(u);
        return Map.of("token", u.token, "name", u.getName(), "role", u.role);
    }
    private static String clean(String s) { return s == null ? "" : s.trim(); }
    private static ResponseStatusException bad(String m) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, m); }
}
