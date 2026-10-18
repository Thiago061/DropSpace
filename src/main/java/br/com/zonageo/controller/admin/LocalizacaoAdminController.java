package br.com.zonageo.controller.admin;

import br.com.zonageo.model.Localizacao;
import br.com.zonageo.repository.LocalizacaoRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/localizacoes")
public class LocalizacaoAdminController {
    private final LocalizacaoRepository localizacoes;

    public LocalizacaoAdminController(LocalizacaoRepository localizacoes) {
        this.localizacoes = localizacoes;
    }

    @GetMapping
    public List<Localizacao> listar() {
        return localizacoes.findAll();
    }

    @GetMapping("/{id}")
    public Localizacao buscar(@PathVariable Long id) {
        return localizacoes.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Localização não encontrada."));
    }

    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public Localizacao criar(@Valid @RequestBody Localizacao item) {
        return localizacoes.save(item);
    }

    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Long id) {
        if (!localizacoes.existsById(id)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        localizacoes.deleteById(id);
    }
}
