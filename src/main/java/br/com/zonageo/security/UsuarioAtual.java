package br.com.zonageo.security;

import br.com.zonageo.model.Usuario;
import br.com.zonageo.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/** Carrega o usuário do banco a partir do token, garantindo que ainda existe e está ativo. */
@Component
public class UsuarioAtual {
    private final UsuarioRepository usuarios;

    public UsuarioAtual(UsuarioRepository usuarios) {
        this.usuarios = usuarios;
    }

    public Usuario exigir(Jwt jwt) {
        try {
            return usuarios.findById(Long.parseLong(jwt.getSubject()))
                    .filter(Usuario::isAtivo)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sessão inválida."));
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sessão inválida.");
        }
    }
}
