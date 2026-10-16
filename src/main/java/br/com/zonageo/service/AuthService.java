package br.com.zonageo.service;

import br.com.zonageo.dto.AuthDtos.*;
import br.com.zonageo.model.Papel;
import br.com.zonageo.model.Plano;
import br.com.zonageo.model.Usuario;
import br.com.zonageo.repository.UsuarioRepository;
import br.com.zonageo.security.LoginAttemptService;
import br.com.zonageo.security.TokenService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

@Service
public class AuthService {
    /** Hash descartável para gastar o mesmo tempo quando o usuário não existe. */
    private final String hashFalso;
    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;
    private final TokenService tokens;
    private final LoginAttemptService tentativas;

    public AuthService(UsuarioRepository usuarios, PasswordEncoder encoder,
                       TokenService tokens, LoginAttemptService tentativas) {
        this.usuarios = usuarios;
        this.encoder = encoder;
        this.tokens = tokens;
        this.tentativas = tentativas;
        this.hashFalso = encoder.encode("senha-inexistente-zonageo");
    }

    public static String normalizarLogin(String login) {
        return login.trim().toLowerCase(Locale.ROOT);
    }

    public TokenResposta registrar(RegistroRequest req) {
        String login = normalizarLogin(req.email());
        if (usuarios.existsByLogin(login)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Este e-mail já está cadastrado.");
        }
        Usuario u = new Usuario();
        u.setNome(req.nome().trim());
        u.setLogin(login);
        u.setSenhaHash(encoder.encode(req.senha()));
        u.setPapel(Papel.USER);
        u.setPlano(Plano.FREE);
        try {
            u = usuarios.save(u);
        } catch (DataIntegrityViolationException e) { // corrida entre dois cadastros iguais
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Este e-mail já está cadastrado.");
        }
        return resposta(u);
    }

    public TokenResposta login(LoginRequest req, String ip) {
        String login = normalizarLogin(req.login());
        String chave = login + "|" + ip;
        if (tentativas.bloqueado(chave)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Muitas tentativas. Aguarde alguns minutos e tente novamente.");
        }
        Usuario u = usuarios.findByLogin(login).orElse(null);
        String hash = u != null ? u.getSenhaHash() : hashFalso;
        boolean senhaOk = encoder.matches(req.senha(), hash);
        if (u == null || !senhaOk || !u.isAtivo()) {
            tentativas.falha(chave);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário ou senha inválidos.");
        }
        tentativas.sucesso(chave);
        return resposta(u);
    }

    private TokenResposta resposta(Usuario u) {
        TokenService.Emitido t = tokens.emitir(u);
        return new TokenResposta(t.token(), "Bearer", t.expiraEm(), UsuarioDto.de(u));
    }
}
