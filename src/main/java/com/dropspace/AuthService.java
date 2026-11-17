package com.dropspace;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
class AuthService {
    private static final int MAX_ATTEMPTS = 5;
    private static final long WINDOW_MS = 5 * 60 * 1000;
    private final Users users;
    private final Map<String, long[]> attempts = new ConcurrentHashMap<>(); // {falhas, início da janela}

    AuthService(Users users) { this.users = users; }

    /** Usuário logado (token válido e não expirado). */
    User user(String header) {
        if (header == null || !header.startsWith("Bearer "))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Faça login para continuar");
        User u = users.findByToken(header.substring(7))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sessão expirada. Entre novamente"));
        if (u.tokenExpires == null || u.tokenExpires.isBefore(LocalDateTime.now()))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sessão expirada. Entre novamente");
        return u;
    }

    /** Todas as rotas /api/admin passam por aqui: o perfil é verificado no servidor, nunca no navegador. */
    User admin(String header) {
        User u = user(header);
        if (!"ADMIN".equals(u.role)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso restrito a administradores");
        return u;
    }

    /** Bloqueia tentativas em excesso (força bruta). */
    void guard(String key) {
        long[] a = attempts.get(key);
        if (a != null && System.currentTimeMillis() - a[1] > WINDOW_MS) { attempts.remove(key); return; }
        if (a != null && a[0] >= MAX_ATTEMPTS)
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Muitas tentativas. Aguarde 5 minutos");
    }
    void fail(String key) {
        attempts.compute(key, (k, a) -> a == null ? new long[]{1, System.currentTimeMillis()} : new long[]{a[0] + 1, a[1]});
    }
    void clear(String key) { attempts.remove(key); }
}
