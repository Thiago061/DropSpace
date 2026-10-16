package br.com.zonageo.security;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limita tentativas de login (força bruta). Em memória: suficiente para uma instância.
 * Com várias instâncias, trocar por Redis ou bucket compartilhado.
 */
@Component
public class LoginAttemptService {
    private static final int MAX_FALHAS = 5;
    private static final Duration JANELA = Duration.ofMinutes(15);

    private record Estado(int falhas, Instant inicio) {}

    private final ConcurrentHashMap<String, Estado> estados = new ConcurrentHashMap<>();

    public boolean bloqueado(String chave) {
        Estado e = estados.get(chave);
        if (e == null) return false;
        if (e.inicio().plus(JANELA).isBefore(Instant.now())) {
            estados.remove(chave);
            return false;
        }
        return e.falhas() >= MAX_FALHAS;
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
