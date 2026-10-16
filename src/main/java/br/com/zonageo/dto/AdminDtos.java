package br.com.zonageo.dto;

import br.com.zonageo.model.Plano;
import br.com.zonageo.model.TipoPonto;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class AdminDtos {
    private AdminDtos() {}

    public record PontoRequest(
            @NotBlank @Size(max = 120) String nome,
            @NotNull TipoPonto tipo,
            @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
            @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude,
            @Size(max = 500) String descricao,
            @Size(max = 200) String endereco,
            @Size(max = 120) String horario,
            boolean gratuito,
            boolean sombra,
            @Size(max = 160) String voucherDescricao) {}

    /** Para FREE, "dias" é ignorado. Para PRO, define por quantos dias o plano vale. */
    public record PlanoRequest(@NotNull Plano plano, @Min(1) @Max(3650) Integer dias) {}

    public record ResgateRequest(@NotBlank @Size(max = 20) String codigo) {}
}
