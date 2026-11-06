package br.com.zonageo.controller.admin;

import br.com.zonageo.dto.AdminDtos.OfertaDto;
import br.com.zonageo.dto.AdminDtos.OfertaRequest;
import br.com.zonageo.dto.AdminDtos.ProdutoCatalogoDto;
import br.com.zonageo.service.OfertaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Cadastro das ofertas de voucher dos mercados parceiros. */
@RestController
@RequestMapping("/api/admin/ofertas")
public class OfertaAdminController {
    private final OfertaService ofertas;

    public OfertaAdminController(OfertaService ofertas) {
        this.ofertas = ofertas;
    }

    @GetMapping
    public List<OfertaDto> listar() {
        return ofertas.listar();
    }

    @GetMapping("/produtos")
    public List<ProdutoCatalogoDto> catalogoProdutos() {
        return ofertas.catalogoProdutos();
    }

    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public OfertaDto criar(@Valid @RequestBody OfertaRequest req) {
        return ofertas.criar(req);
    }

    @PutMapping("/{id}")
    public OfertaDto atualizar(@PathVariable Long id, @Valid @RequestBody OfertaRequest req) {
        return ofertas.atualizar(id, req);
    }

    @PatchMapping("/{id}/ativo")
    public OfertaDto alternarAtivo(@PathVariable Long id, @RequestParam boolean ativo) {
        return ofertas.alternarAtivo(id, ativo);
    }
}
