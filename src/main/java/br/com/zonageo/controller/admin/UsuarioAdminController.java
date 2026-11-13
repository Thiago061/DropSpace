package br.com.zonageo.controller.admin;

import br.com.zonageo.dto.AdminDtos.*;
import br.com.zonageo.service.UsuarioAdminService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Usuários cadastrados, planos e histórico de acessos. */
@RestController
@RequestMapping("/api/admin")
public class UsuarioAdminController {
    private final UsuarioAdminService usuarios;

    public UsuarioAdminController(UsuarioAdminService usuarios) {
        this.usuarios = usuarios;
    }

    @GetMapping("/usuarios")
    public PaginaDto<UsuarioAdminDto> listar(@RequestParam(defaultValue = "") String q,
                                             @RequestParam(defaultValue = "0") int pagina,
                                             @RequestParam(defaultValue = "20") int tamanho) {
        return usuarios.listar(q, pagina, tamanho);
    }

    @GetMapping("/usuarios/resumo")
    public ResumoUsuarios resumo() {
        return usuarios.resumo();
    }

    @PatchMapping("/usuarios/{id}/plano")
    public UsuarioAdminDto alterarPlano(@PathVariable Long id, @Valid @RequestBody PlanoRequest req) {
        return usuarios.alterarPlano(id, req);
    }

    @PatchMapping("/usuarios/{id}/ativo")
    public UsuarioAdminDto alterarAtivo(@PathVariable Long id, @RequestParam boolean ativo) {
        return usuarios.alterarAtivo(id, ativo);
    }

    @GetMapping("/acessos")
    public List<AcessoDto> acessos() {
        return usuarios.acessosRecentes();
    }
}
