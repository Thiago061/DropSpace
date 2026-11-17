package com.dropspace;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** Consulta o CEP no ViaCEP e calcula o frete por região. Tabela de exemplo: troque por API de transportadora/Correios. */
@Service
class ShippingService {
    record Address(String cep, String street, String district, String city, String uf) {}
    record Quote(String cep, String city, String uf, BigDecimal price, int days) {}
    private record Rate(String price, int days) {}

    private static final BigDecimal FREE_ABOVE = new BigDecimal("3000");
    private static final Rate SUDESTE = new Rate("24.90", 4), SUL = new Rate("29.90", 5), CENTRO = new Rate("34.90", 6),
        NORDESTE = new Rate("39.90", 8), NORTE = new Rate("49.90", 10);
    private static final Map<String, Rate> BY_UF = Map.ofEntries(
        Map.entry("SP", new Rate("19.90", 2)), Map.entry("RJ", SUDESTE), Map.entry("MG", SUDESTE), Map.entry("ES", SUDESTE),
        Map.entry("PR", SUL), Map.entry("SC", SUL), Map.entry("RS", SUL),
        Map.entry("DF", CENTRO), Map.entry("GO", CENTRO), Map.entry("MT", CENTRO), Map.entry("MS", CENTRO),
        Map.entry("BA", NORDESTE), Map.entry("SE", NORDESTE), Map.entry("AL", NORDESTE), Map.entry("PE", NORDESTE),
        Map.entry("PB", NORDESTE), Map.entry("RN", NORDESTE), Map.entry("CE", NORDESTE), Map.entry("PI", NORDESTE), Map.entry("MA", NORDESTE),
        Map.entry("AM", NORTE), Map.entry("PA", NORTE), Map.entry("AC", NORTE), Map.entry("RO", NORTE),
        Map.entry("RR", NORTE), Map.entry("AP", NORTE), Map.entry("TO", NORTE));

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(4)).build();
    private final ObjectMapper mapper = new ObjectMapper();

    /** Consulta o endereço de um CEP no ViaCEP. */
    Address lookup(String rawCep) {
        String cep = rawCep == null ? "" : rawCep.replaceAll("\\D", "");
        if (cep.length() != 8) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CEP inválido. Digite 8 números");
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create("https://viacep.com.br/ws/" + cep + "/json/")).timeout(Duration.ofSeconds(5)).build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            JsonNode n = res.statusCode() == 200 ? mapper.readTree(res.body()) : null;
            if (n == null || n.has("erro")) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "CEP não encontrado");
            return new Address(cep, n.path("logradouro").asText(), n.path("bairro").asText(), n.path("localidade").asText(), n.path("uf").asText());
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Não foi possível consultar o CEP agora. Tente novamente");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Consulta interrompida. Tente novamente");
        }
    }

    Quote quote(String rawCep, BigDecimal subtotal) {
        Address a = lookup(rawCep);
        Rate rate = BY_UF.get(a.uf());
        if (rate == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Não entregamos nesta região");
        boolean free = subtotal != null && subtotal.compareTo(FREE_ABOVE) >= 0;
        return new Quote(a.cep(), a.city(), a.uf(), free ? BigDecimal.ZERO : new BigDecimal(rate.price()), rate.days());
    }
}
