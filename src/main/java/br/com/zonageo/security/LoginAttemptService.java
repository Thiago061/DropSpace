package br.com.zonageo.security;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limita tentativas de login (força bruta), por usuário + IP. Em memória: o bloqueio some ao reiniciar
 * o servidor e vale para uma instância só. Com várias instâncias, trocar por Redis.
 */
@Component
public class LoginAttemptService {
    private static final int MAX_FALHAS = 10;
    private static final Duration JANELA = Duration.ofMinutes(10);

    private record Estado(int falhas, Instant inicio) {}

    private final ConcurrentHashMap<String, Estado> estados = new ConcurrentHashMap<>();

    public boolean bloqueado(String chave) {
        return minutosRestantes(chave) > 0;
    }

    /** Minutos até liberar (0 se não está bloqueado). */
    public long minutosRestantes(String chave) {
        Estado e = estados.get(chave);
        if (e == null) return 0;
        Instant fim = e.inicio().plus(JANELA);
        if (fim.isBefore(Instant.now())) {
            estados.remove(chave);
            return 0;
        }
        if (e.falhas() < MAX_FALHAS) return 0;
        return Math.max(1, Duration.between(Instant.now(), fim).toMinutes() + 1);
    }

    public void falha(String chave) {
        if (estados.size() > 10_000) estados.clear(); // proteção simples contra crescimento ilimitado
        estados.merge(chave, new Estado(1, Instant.now()),
                (atual, novo) -> atual.inicio().plus(JANELA).isBefore(Instant.now())
                        ? novo : new Estado(atual.falhas() + 1, atual.inicio()));
    }

    public void sucesso(String chave) {
        estados.remove(chave);
    }
}
