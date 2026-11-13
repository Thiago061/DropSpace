package br.com.zonageo.service;

import br.com.zonageo.dto.AdminDtos.*;
import br.com.zonageo.model.Papel;
import br.com.zonageo.model.Plano;
import br.com.zonageo.model.Usuario;
import br.com.zonageo.repository.AcessoRepository;
import br.com.zonageo.repository.UsuarioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/** Gestão de usuários no painel: listagem, resumo, plano, ativação e histórico de acessos. */
@Service
public class UsuarioAdminService {
    private final UsuarioRepository usuarios;
    private final AcessoRepository acessos;

    public UsuarioAdminService(UsuarioRepository usuarios, AcessoRepository acessos) {
        this.usuarios = usuarios;
        this.acessos = acessos;
    }

    @Transactional(readOnly = true)
    public PaginaDto<UsuarioAdminDto> listar(String busca, int pagina, int tamanho) {
        String q = busca == null ? "" : busca.trim();
        if (q.length() > 100) q = q.substring(0, 100);
        int p = Math.max(0, pagina);
        int t = Math.max(1, Math.min(tamanho, 100));
        Page<Usuario> resultado = usuarios.findByNomeContainingIgnoreCaseOrLoginContainingIgnoreCase(
                q, q, PageRequest.of(p, t, Sort.by(Sort.Direction.DESC, "criadoEm")));
        return new PaginaDto<>(resultado.getContent().stream().map(UsuarioAdminDto::de).toList(),
                resultado.getTotalElements(), p, t);
    }

    @Transactional(readOnly = true)
    public ResumoUsuarios resumo() {
        Instant agora = Instant.now();
        Instant semana = agora.minus(7, ChronoUnit.DAYS);
        return new ResumoUsuarios(usuarios.count(), usuarios.countByCriadoEmGreaterThanEqual(semana),
                usuarios.countByUltimoLoginEmGreaterThanEqual(semana), usuarios.contarProVigentes(agora),
                usuarios.countByAtivoFalse());
    }

    /**
     * Libera ou encerra o plano Pro manualmente. Enquanto o pagamento online não existe,
     * é por aqui que um assinante ganha acesso.
     */
    @Transactional
    public UsuarioAdminDto alterarPlano(Long id, PlanoRequest req) {
        Usuario u = buscar(id);
        if (u.getPapel() == Papel.ADMIN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Administradores não possuem plano.");
        }
        if (req.plano() == Plano.PRO) {
            if (req.dias() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe por quantos dias o plano Pro vale.");
            }
            u.setPlano(Plano.PRO);
            u.setPlanoExpiraEm(Instant.now().plus(req.dias(), ChronoUnit.DAYS));
        } else {
            u.setPlano(Plano.FREE);
            u.setPlanoExpiraEm(null);
        }
        return UsuarioAdminDto.de(usuarios.save(u));
    }

    @Transactional
    public UsuarioAdminDto alterarAtivo(Long id, boolean ativo) {
        Usuario u = buscar(id);
        if (u.getPapel() == Papel.ADMIN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Não é possível desativar um administrador por aqui.");
        }
        u.setAtivo(ativo);
        return UsuarioAdminDto.de(usuarios.save(u));
    }

    @Transactional(readOnly = true)
    public List<AcessoDto> acessosRecentes() {
        return acessos.findTop100ByOrderByCriadoEmDesc().stream().map(AcessoDto::de).toList();
    }

    private Usuario buscar(Long id) {
        return usuarios.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado."));
    }
}
