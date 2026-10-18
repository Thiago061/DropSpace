package br.com.zonageo.dto;

import br.com.zonageo.model.Acesso;
import br.com.zonageo.model.OfertaVoucher;
import br.com.zonageo.model.Papel;
import br.com.zonageo.model.Plano;
import br.com.zonageo.model.TipoPonto;
import br.com.zonageo.model.Usuario;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.math.BigDecimal;
import java.util.List;

/** Contratos do painel administrativo, agrupados por assunto. */
public final class AdminDtos {
    private AdminDtos() {}

    // ------------------------------------------------------------ pontos
    public record PontoRequest(
            @NotBlank @Size(max = 120) String nome,
            @NotNull TipoPonto tipo,
            @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
            @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude,
            @Size(max = 500) String descricao,
            @Size(max = 200) String endereco,
            @Size(max = 120) String horario,
            boolean gratuito,
            boolean sombra) {}

    // ------------------------------------------------------------ ofertas de voucher
    public record OfertaRequest(
            @NotNull Long pontoId,
            @NotBlank @Size(max = 120) String titulo,
            @Size(max = 300) String descricao,
            @NotBlank @Size(max = 80) String unidade,
            @NotNull @DecimalMin("0.00") @Digits(integer = 8, fraction = 2) BigDecimal valor) {}

    public record OfertaDto(Long id, Long pontoId, String pontoNome, String titulo, String descricao,
                            String unidade, BigDecimal valor, boolean ativo, boolean vigente) {
        public static OfertaDto de(OfertaVoucher o, String pontoNome) {
            return new OfertaDto(o.getId(), o.getPontoId(), pontoNome, o.getTitulo(), o.getDescricao(),
                    o.getUnidade(), o.getValor(), o.isAtivo(), o.vigente());
        }
    }

    public record ProdutoCatalogoDto(String nome, String unidade, BigDecimal valorSugerido, String descricao) {}

    public record ResgateRequest(@NotBlank @Size(max = 20) String codigo) {}

    // ------------------------------------------------------------ usuários
    /** Para FREE, "dias" é ignorado. Para PRO, define por quantos dias o plano vale. */
    public record PlanoRequest(@NotNull Plano plano, @Min(1) @Max(3650) Integer dias) {}

    public record UsuarioAdminDto(Long id, String nome, String login, Papel papel, Plano plano,
                                  Instant planoExpiraEm, boolean ativo, Instant criadoEm,
                                  Instant ultimoLoginEm, int totalLogins) {
        public static UsuarioAdminDto de(Usuario u) {
            return new UsuarioAdminDto(u.getId(), u.getNome(), u.getLogin(), u.getPapel(), u.planoEfetivo(),
                    u.getPlanoExpiraEm(), u.isAtivo(), u.getCriadoEm(), u.getUltimoLoginEm(), u.getTotalLogins());
        }
    }

    public record ResumoUsuarios(long total, long novos7Dias, long ativos7Dias, long pro, long desativados) {}

    public record AcessoDto(Long usuarioId, String login, boolean sucesso, String ip, Instant criadoEm) {
        public static AcessoDto de(Acesso a) {
            return new AcessoDto(a.getUsuarioId(), a.getLogin(), a.isSucesso(), a.getIp(), a.getCriadoEm());
        }
    }

    public record PaginaDto<T>(List<T> itens, long total, int pagina, int tamanho) {}

    // ------------------------------------------------------------ busca de lugares
    public record LugarDto(String nome, String endereco, double latitude, double longitude) {}

    /** "fonte" diz quem respondeu; "aviso" explica quando houve alternativa ou falha. */
    public record ResultadoBusca(String fonte, List<LugarDto> lugares, String aviso) {}
}
