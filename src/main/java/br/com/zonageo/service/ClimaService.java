package br.com.zonageo.service;

import br.com.zonageo.service.RecomendacaoEngine.Clima;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Clima atual via Open-Meteo, com cache curto (~1 km, 10 min) e timeouts.
 * Atenção: o plano gratuito da Open-Meteo é para uso não comercial. Como o Zona GEO vende planos,
 * contrate o plano comercial (ou outro provedor) antes de lançar.
 */
@Service
public class ClimaService {
    private static final Logger log = LoggerFactory.getLogger(ClimaService.class);
    private static final long TTL_SEGUNDOS = 600;

    private record Entrada(Clima clima, Instant expiraEm) {}

    private final RestClient http;
    private final Map<String, Entrada> cache = new ConcurrentHashMap<>();

    public ClimaService(RestClient.Builder builder) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(4000);
        this.http = builder.baseUrl("https://api.open-meteo.com").requestFactory(factory).build();
    }

    public Optional<Clima> obter(double lat, double lon) {
        String chave = String.format(Locale.ROOT, "%.2f,%.2f", lat, lon);
        Entrada em = cache.get(chave);
        if (em != null && em.expiraEm().isAfter(Instant.now())) return Optional.of(em.clima());
        try {
            JsonNode raiz = http.get()
                    .uri(u -> u.path("/v1/forecast")
                            .queryParam("latitude", lat)
                            .queryParam("longitude", lon)
                            .queryParam("current", "temperature_2m,relative_humidity_2m,apparent_temperature,uv_index")
                            .queryParam("timezone", "UTC")
                            .build())
                    .retrieve()
                    .body(JsonNode.class);
            Optional<Clima> clima = converter(raiz);
            clima.ifPresent(c -> {
                if (cache.size() > 2000) cache.clear();
                cache.put(chave, new Entrada(c, Instant.now().plusSeconds(TTL_SEGUNDOS)));
            });
            return clima;
        } catch (Exception e) {
            log.warn("Falha ao consultar clima: {}", e.getMessage());
            return Optional.empty();
        }
    }

    static Optional<Clima> converter(JsonNode raiz) {
        if (raiz == null) return Optional.empty();
        JsonNode atual = raiz.path("current");
        if (!atual.hasNonNull("temperature_2m")) return Optional.empty();
        double temp = atual.get("temperature_2m").asDouble();
        double sensacao = atual.hasNonNull("apparent_temperature") ? atual.get("apparent_temperature").asDouble() : temp;
        Double umidade = atual.hasNonNull("relative_humidity_2m") ? atual.get("relative_humidity_2m").asDouble() : null;
        Double uv = atual.hasNonNull("uv_index") ? atual.get("uv_index").asDouble() : null;
        Instant momento = Instant.now();
        try {
            if (atual.hasNonNull("time")) {
                momento = java.time.LocalDateTime.parse(atual.get("time").asText()).toInstant(ZoneOffset.UTC);
            }
        } catch (DateTimeParseException ignored) { /* mantém o horário atual */ }
        return Optional.of(new Clima(temp, sensacao, umidade, uv, momento, "Open-Meteo"));
    }
}
