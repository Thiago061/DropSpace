package br.com.zonageo.service;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import static br.com.zonageo.dto.AppDtos.ItemRecomendacaoResumo;

/** Gera um resumo opcional enviando somente rótulos de categoria e prioridade ao Gemini. */
@Service
public class GeminiRecomendacaoService {
    private static final String AVISO = "Resumo gerado por IA. As recomendações originais e os alertas oficiais têm prioridade.";

    private final Client client;
    private final String model;

    public GeminiRecomendacaoService(
            @Value("${zonageo.gemini.api-key:}") String apiKey,
            @Value("${zonageo.gemini.model:gemini-flash-latest}") String model) {
        this.client = apiKey == null || apiKey.isBlank() ? null : Client.builder().apiKey(apiKey).build();
        this.model = model;
    }

    public String resumir(List<ItemRecomendacaoResumo> recomendacoes) {
        if (client == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Gemini não configurado. Defina GEMINI_API_KEY no arquivo .env e reinicie o backend.");
        }

        String contexto = recomendacoes.stream()
                .map(r -> r.categoria().toLowerCase(Locale.ROOT) + ":" + r.prioridade())
                .distinct()
                .collect(Collectors.joining(", "));
        String prompt = """
                Resuma em português do Brasil, em no máximo 45 palavras, as categorias e prioridades abaixo.
                Explique que os itens com prioridade ALERTA devem ser lidos primeiro.
                Não invente fatos, locais, causas, diagnósticos, telefones ou instruções de emergência.
                Não substitua nem altere as recomendações originais. Se houver categoria de ocorrência,
                diga apenas para consultar a recomendação original e alertas oficiais.
                Categorias e prioridades: %s
                """.formatted(contexto);

        try {
            GenerateContentResponse response = client.models.generateContent(model, prompt, null);
            String resumo = response.text();
            if (resumo == null || resumo.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Gemini não retornou um resumo.");
            }
            return resumo.strip();
        } catch (ResponseStatusException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Não foi possível obter o resumo do Gemini agora.");
        }
    }

    public String aviso() {
        return AVISO;
    }
}
