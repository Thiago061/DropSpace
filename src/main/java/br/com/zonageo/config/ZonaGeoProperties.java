package br.com.zonageo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.math.BigDecimal;
import java.time.Duration;

@ConfigurationProperties(prefix = "zonageo")
public record ZonaGeoProperties(
        @DefaultValue Jwt jwt,
        @DefaultValue Admin admin,
        @DefaultValue Voucher voucher,
        @DefaultValue Planos planos,
        @DefaultValue("false") boolean seedDemo) {

    /** Segredo HMAC (mínimo 32 caracteres). Vem do ambiente, nunca do código. */
    public record Jwt(String secret, @DefaultValue("PT8H") Duration ttl) {}

    /** Administrador inicial. Só é criado se usuário e senha forem informados no ambiente. */
    public record Admin(String user, String password) {}

    public record Voucher(@DefaultValue("120") int validadeMinutos,
                          @DefaultValue("1") int limiteDiario,
                          @DefaultValue("500") int raioValidacaoM) {}

    public record Planos(@DefaultValue("9.90") BigDecimal proPrecoMensal) {}
}
