package br.com.zonageo.service;

import br.com.zonageo.dto.AuthDtos.*;
import br.com.zonageo.model.Acesso;
import br.com.zonageo.model.Papel;
import br.com.zonageo.model.Plano;
import br.com.zonageo.model.Usuario;
import br.com.zonageo.repository.AcessoRepository;
import br.com.zonageo.repository.UsuarioRepository;
import br.com.zonageo.security.LoginAttemptService;
import br.com.zonageo.security.TokenService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

/**
 * Cadastro e login. Obs.: os métodos não são @Transactional de propósito: o registro de uma tentativa
 * falha precisa ser gravado mesmo que, logo em seguida, lancemos a exceção de "credenciais inválidas".
 */
@Service
public class AuthService {
    /** Hash descartável para gastar o mesmo tempo quando o usuário não existe. */
    private final String hashFalso;
    private final UsuarioRepository usuarios;
    private final AcessoRepository acessos;
    private final PasswordEncoder encoder;
    private final TokenService tokens;
    private final LoginAttemptService tentativas;

    public AuthService(UsuarioRepository usuarios, AcessoRepository acessos, PasswordEncoder encoder,
                       TokenService tokens, LoginAttemptService tentativas) {
        this.usuarios = usuarios;
        this.acessos = acessos;
        this.encoder = encoder;
        this.tokens = tokens;
        this.tentativas = tentativas;
        this.hashFalso = encoder.encode("senha-inexistente-zonageo");
    }

    public static String normalizarLogin(String login) {
        return login.trim().toLowerCase(Locale.ROOT);
    }

    public TokenResposta registrar(RegistroRequest req, String ip) {
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
        u.registrarAcesso();
        try {
            u = usuarios.save(u);
        } catch (DataIntegrityViolationException e) { // corrida entre dois cadastros iguais
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Este e-mail já está cadastrado.");
        }
        acessos.save(new Acesso(u.getId(), login, true, ip));
        return resposta(u);
    }

    public TokenResposta login(LoginRequest req, String ip) {
        String login = normalizarLogin(req.login());
        String chave = login + "|" + ip;
        long espera = tentativas.minutosRestantes(chave);
        if (espera > 0) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Muitas tentativas de entrada. Tente novamente em " + espera + " min.");
        }
        Usuario u = usuarios.findByLogin(login).orElse(null);
        boolean senhaOk = encoder.matches(req.senha(), u != null ? u.getSenhaHash() : hashFalso);
        if (u == null || !senhaOk || !u.isAtivo()) {
            tentativas.falha(chave);
            acessos.save(new Acesso(u != null ? u.getId() : null, login, false, ip));
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário ou senha inválidos.");
        }
        tentativas.sucesso(chave);
        u.registrarAcesso();
        usuarios.save(u);
        acessos.save(new Acesso(u.getId(), login, true, ip));
        return resposta(u);
    }

    private TokenResposta resposta(Usuario u) {
        TokenService.Emitido t = tokens.emitir(u);
        return new TokenResposta(t.token(), "Bearer", t.expiraEm(), UsuarioDto.de(u));
    }
}
