package br.com.zonageo.controller;

import br.com.zonageo.dto.AdminDtos.*;
import br.com.zonageo.dto.AppDtos.VoucherDto;
import br.com.zonageo.dto.AuthDtos.UsuarioDto;
import br.com.zonageo.model.Papel;
import br.com.zonageo.model.Plano;
import br.com.zonageo.model.PontoApoio;
import br.com.zonageo.model.Usuario;
import br.com.zonageo.repository.PontoApoioRepository;
import br.com.zonageo.repository.UsuarioRepository;
import br.com.zonageo.service.VoucherService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/** Painel administrativo. Todas as rotas exigem papel ADMIN (ver SecurityConfig). */
@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final PontoApoioRepository pontos;
    private final UsuarioRepository usuarios;
    private final VoucherService vouchers;

    public AdminController(PontoApoioRepository pontos, UsuarioRepository usuarios, VoucherService vouchers) {
        this.pontos = pontos;
        this.usuarios = usuarios;
        this.vouchers = vouchers;
    }

    // ---- pontos de apoio

    @GetMapping("/pontos")
    public List<PontoApoio> listarPontos() { return pontos.findAllByOrderByNomeAsc(); }

    @PostMapping("/pontos") @ResponseStatus(HttpStatus.CREATED)
    public PontoApoio criarPonto(@Valid @RequestBody PontoRequest req) {
        return pontos.save(aplicar(new PontoApoio(), req));
    }

    @PutMapping("/pontos/{id}")
    public PontoApoio atualizarPonto(@PathVariable Long id, @Valid @RequestBody PontoRequest req) {
        PontoApoio p = pontos.findById(id).orElseThrow(this::pontoNaoEncontrado);
        return pontos.save(aplicar(p, req));
    }

    /** Ativa/desativa o ponto. Não apaga, para preservar o histórico de vouchers. */
    @PatchMapping("/pontos/{id}/ativo")
    public PontoApoio alternarAtivo(@PathVariable Long id, @RequestParam boolean ativo) {
        PontoApoio p = pontos.findById(id).orElseThrow(this::pontoNaoEncontrado);
        p.setAtivo(ativo);
        return pontos.save(p);
    }

    private ResponseStatusException pontoNaoEncontrado() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Ponto não encontrado.");
    }

    private static PontoApoio aplicar(PontoApoio p, PontoRequest r) {
        p.setNome(r.nome().trim());
        p.setTipo(r.tipo());
        p.setLatitude(r.latitude());
        p.setLongitude(r.longitude());
        p.setDescricao(vazioParaNulo(r.descricao()));
        p.setEndereco(vazioParaNulo(r.endereco()));
        p.setHorario(vazioParaNulo(r.horario()));
        p.setGratuito(r.gratuito());
        p.setSombra(r.sombra());
        // só mercados parceiros oferecem voucher
        p.setVoucherDescricao(r.tipo() == br.com.zonageo.model.TipoPonto.MERCADO_PARCEIRO
                ? vazioParaNulo(r.voucherDescricao()) : null);
        return p;
    }

    private static String vazioParaNulo(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    // ---- usuários e planos

    @GetMapping("/usuarios")
    public List<UsuarioDto> listarUsuarios() {
        return usuarios.findTop200ByPapelOrderByCriadoEmDesc(Papel.USER).stream().map(UsuarioDto::de).toList();
    }

    /**
     * Libera ou encerra o plano Pro manualmente. Enquanto o pagamento online não existe,
     * é por aqui que um assinante ganha acesso.
     */
    @PatchMapping("/usuarios/{id}/plano")
    public UsuarioDto alterarPlano(@PathVariable Long id, @Valid @RequestBody PlanoRequest req) {
        Usuario u = usuarios.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado."));
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
        return UsuarioDto.de(usuarios.save(u));
    }

    // ---- vouchers

    @GetMapping("/vouchers")
    public List<VoucherDto> listarVouchers() { return vouchers.recentes(); }

    @PostMapping("/vouchers/resgatar")
    public VoucherDto resgatar(@Valid @RequestBody ResgateRequest req) {
        return vouchers.resgatar(req.codigo());
    }
}
