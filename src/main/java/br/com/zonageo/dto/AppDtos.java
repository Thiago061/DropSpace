package br.com.zonageo.dto;

import br.com.zonageo.model.StatusVoucher;
import br.com.zonageo.model.TipoPonto;
import br.com.zonageo.service.RecomendacaoEngine.Clima;
import br.com.zonageo.service.RecomendacaoEngine.OcorrenciaProxima;
import br.com.zonageo.service.RecomendacaoEngine.PontoProximo;
import br.com.zonageo.service.RecomendacaoEngine.Recomendacao;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;

public final class AppDtos {
    private AppDtos() {}

    public static final String AVISO = "Informações de apoio, que não substituem alertas oficiais. "
            + "Emergências: 193 (Bombeiros), 192 (SAMU), 199 (Defesa Civil).";

    public record LocalidadeResposta(double latitude, double longitude, int raioM, String plano,
                                     Clima clima, String nivelTermico, String nivelRotulo,
                                     List<Recomendacao> recomendacoes,
                                     List<OcorrenciaProxima> ocorrencias,
                                     List<PontoProximo> pontos, String aviso) {}

    public record Parada(Long id, String nome, TipoPonto tipo, double latitude, double longitude,
                         double distanciaDaOrigemM, double desvioM, boolean sombra, boolean gratuito,
                         String voucher) {}

    public record AlertaRota(String titulo, String categoria, String severidade, double desvioM, String sugestao) {}

    public record RotaResposta(double distanciaDiretaM, String modo, List<Parada> paradas,
                               List<AlertaRota> alertas, String linkMapa, String aviso) {}

    public record VoucherRequest(
            @NotNull Long pontoId,
            @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
            @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude) {}

    public record VoucherDto(Long id, String codigo, Long pontoId, String pontoNome, String descricao,
                             String status, Instant emitidoEm, Instant expiraEm, Instant resgatadoEm,
                             Long usuarioId) {}

    public record PlanoInfo(String codigo, String nome, java.math.BigDecimal precoMensal, List<String> recursos) {}

    /** Usado só para tipar o status do voucher nas respostas. */
    public static String statusEfetivo(StatusVoucher status, Instant expiraEm) {
        if (status == StatusVoucher.ATIVO && expiraEm.isBefore(Instant.now())) return "EXPIRADO";
        return status.name();
    }
}
