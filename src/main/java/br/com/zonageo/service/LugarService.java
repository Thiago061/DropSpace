package br.com.zonageo.service;

import br.com.zonageo.config.ZonaGeoProperties;
import br.com.zonageo.dto.AdminDtos.LugarDto;
import br.com.zonageo.dto.AdminDtos.ResultadoBusca;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Busca de lugares por nome ("Sobradinho", "Atacadão Dia a Dia"...) para o painel admin.
 *
 * <p>Usa a Places API (New) do Google, chamada pelo servidor: a chave nunca vai para o navegador.
 * Sem chave (ou se o Google falhar), usa o Nominatim do OpenStreetMap, que é gratuito mas menos
 * completo para comércios. As buscas só acontecem quando o admin pede (botão/Enter), nunca a cada tecla.
 */
@Service
public class LugarService {
    private static final Logger log = LoggerFactory.getLogger(LugarService.class);

    private final RestClient google;
    private final RestClient osm;
    private final String chaveGoogle;

    public LugarService(RestClient.Builder builder, ZonaGeoProperties props) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(5000);
        this.google = builder.clone().baseUrl("https://places.googleapis.com").requestFactory(factory).build();
        this.osm = builder.clone().baseUrl("https://nominatim.openstreetmap.org").requestFactory(factory)
                .defaultHeader("User-Agent", "ZonaGEO-Admin/1.0").build();
        this.chaveGoogle = props.google().apiKey();
    }

    public ResultadoBusca buscar(String consulta) {
        String aviso = null;
        if (chaveGoogle == null || chaveGoogle.isBlank()) {
            aviso = "Busca do Google não configurada (defina GOOGLE_MAPS_API_KEY). Usando OpenStreetMap.";
        } else {
            try {
                return new ResultadoBusca("Google", buscarNoGoogle(consulta), null);
            } catch (Exception e) {
                log.warn("Falha na busca do Google: {}", e.getMessage());
                aviso = "O Google não respondeu (confira a chave e se a Places API (New) está ativada). Usando OpenStreetMap.";
            }
        }
        try {
            return new ResultadoBusca("OpenStreetMap", buscarNoOsm(consulta), aviso);
        } catch (Exception e) {
            log.warn("Falha na busca do OpenStreetMap: {}", e.getMessage());
            return new ResultadoBusca("Nenhuma", List.of(), "Não foi possível buscar agora. Tente novamente ou marque no mapa.");
        }
    }

    private List<LugarDto> buscarNoGoogle(String consulta) {
        JsonNode raiz = google.post()
                .uri("/v1/places:searchText")
                .header("X-Goog-Api-Key", chaveGoogle)
                .header("X-Goog-FieldMask", "places.displayName,places.formattedAddress,places.location")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("textQuery", consulta, "languageCode", "pt-BR", "regionCode", "BR", "pageSize", 6))
                .retrieve()
                .body(JsonNode.class);
        List<LugarDto> lugares = new ArrayList<>();
        if (raiz == null) return lugares;
        for (JsonNode p : raiz.path("places")) {
            JsonNode loc = p.path("location");
            if (!loc.hasNonNull("latitude") || !loc.hasNonNull("longitude")) continue;
            lugares.add(new LugarDto(p.path("displayName").path("text").asText(""),
                    p.path("formattedAddress").asText(""), loc.get("latitude").asDouble(), loc.get("longitude").asDouble()));
        }
        return lugares;
    }

    private List<LugarDto> buscarNoOsm(String consulta) {
        JsonNode raiz = osm.get()
                .uri(u -> u.path("/search")
                        .queryParam("q", consulta)
                        .queryParam("format", "jsonv2")
                        .queryParam("limit", 6)
                        .queryParam("countrycodes", "br")
                        .queryParam("accept-language", "pt-BR")
                        .build())
                .retrieve()
                .body(JsonNode.class);
        List<LugarDto> lugares = new ArrayList<>();
        if (raiz == null) return lugares;
        for (JsonNode p : raiz) {
            String endereco = p.path("display_name").asText("");
            String nome = p.path("name").asText("");
            if (nome.isBlank()) nome = endereco.split(",")[0].trim();
            try {
                lugares.add(new LugarDto(nome, endereco,
                        Double.parseDouble(p.path("lat").asText()), Double.parseDouble(p.path("lon").asText())));
            } catch (NumberFormatException ignored) { /* resultado sem coordenadas válidas */ }
        }
        return lugares;
    }
}
