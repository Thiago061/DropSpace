package br.com.zonageo.controller.admin;

import br.com.zonageo.dto.AdminDtos.PontoRequest;
import br.com.zonageo.model.PontoApoio;
import br.com.zonageo.service.PontoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Pontos de apoio: água, sombra, abrigo e mercados parceiros. */
@RestController
@RequestMapping("/api/admin/pontos")
public class PontoAdminController {
    private final PontoService pontos;

    public PontoAdminController(PontoService pontos) {
        this.pontos = pontos;
    }

    @GetMapping
    public List<PontoApoio> listar() {
        return pontos.listar();
    }

    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public PontoApoio criar(@Valid @RequestBody PontoRequest req) {
        return pontos.criar(req);
    }

    @PutMapping("/{id}")
    public PontoApoio atualizar(@PathVariable Long id, @Valid @RequestBody PontoRequest req) {
        return pontos.atualizar(id, req);
    }

    @PatchMapping("/{id}/ativo")
    public PontoApoio alternarAtivo(@PathVariable Long id, @RequestParam boolean ativo) {
        return pontos.alternarAtivo(id, ativo);
    }
}
