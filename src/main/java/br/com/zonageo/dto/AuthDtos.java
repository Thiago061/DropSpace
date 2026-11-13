package br.com.zonageo.dto;

import br.com.zonageo.model.Papel;
import br.com.zonageo.model.Plano;
import br.com.zonageo.model.Usuario;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class AuthDtos {
    private AuthDtos() {}

    public record RegistroRequest(
            @NotBlank @Size(max = 120) String nome,
            @NotBlank @Email @Size(max = 160) String email,
            // 72 bytes é o limite do BCrypt
            @NotBlank @Size(min = 8, max = 72, message = "a senha deve ter de 8 a 72 caracteres") String senha) {}

    public record LoginRequest(@NotBlank @Size(max = 160) String login,
                               @NotBlank @Size(max = 72) String senha) {}

    public record UsuarioDto(Long id, String nome, String login, Papel papel, Plano plano, Instant planoExpiraEm) {
        public static UsuarioDto de(Usuario u) {
            return new UsuarioDto(u.getId(), u.getNome(), u.getLogin(), u.getPapel(),
                    u.planoEfetivo(), u.getPlanoExpiraEm());
        }
    }

    public record TokenResposta(String token, String tipo, Instant expiraEm, UsuarioDto usuario) {}
}
