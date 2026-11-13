package br.com.zonageo.controller.admin;

import br.com.zonageo.model.MedicaoAmbiental;
import br.com.zonageo.repository.MedicaoAmbientalRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/medicoes")
public class MedicaoAdminController {
    private final MedicaoAmbientalRepository medicoes;

    public MedicaoAdminController(MedicaoAmbientalRepository medicoes) {
        this.medicoes = medicoes;
    }

    @GetMapping
    public List<MedicaoAmbiental> listar() {
        return medicoes.findAll();
    }

    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public MedicaoAmbiental criar(@Valid @RequestBody MedicaoAmbiental item) {
        double umidade = item.getUmidadePercentual() == null ? 0 : item.getUmidadePercentual().doubleValue();
        if (umidade < 0 || umidade > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Umidade deve estar entre 0 e 100%.");
        }
        if (item.getNomeLocal() != null) item.setNomeLocal(item.getNomeLocal().trim());
        if (item.getCondicao() == null || item.getCondicao().isBlank()) item.setCondicao("Não informado");
        if (item.getFonte() == null || item.getFonte().isBlank()) item.setFonte("Registro manual");
        return medicoes.save(item);
    }

    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Long id) {
        if (!medicoes.existsById(id)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        medicoes.deleteById(id);
    }
}
