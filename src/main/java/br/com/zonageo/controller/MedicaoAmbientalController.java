package br.com.zonageo.controller;
import br.com.zonageo.model.MedicaoAmbiental;
import br.com.zonageo.repository.MedicaoAmbientalRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
@RestController
@RequestMapping("/api/medicoes")
public class MedicaoAmbientalController {
    private final MedicaoAmbientalRepository repository;
    public MedicaoAmbientalController(MedicaoAmbientalRepository repository) { this.repository = repository; }
    @GetMapping public List<MedicaoAmbiental> listar() { return repository.findAll(); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public MedicaoAmbiental criar(@Valid @RequestBody MedicaoAmbiental item) {
        if (item.getUmidadePercentual() != null && (item.getUmidadePercentual().doubleValue() < 0 || item.getUmidadePercentual().doubleValue() > 100)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Umidade deve estar entre 0 e 100%.");
        if (item.getNomeLocal() != null) item.setNomeLocal(item.getNomeLocal().trim());
        if (item.getCondicao() == null || item.getCondicao().isBlank()) item.setCondicao("Não informado");
        if (item.getFonte() == null || item.getFonte().isBlank()) item.setFonte("Registro manual");
        return repository.save(item);
    }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Long id) { if (!repository.existsById(id)) throw new ResponseStatusException(HttpStatus.NOT_FOUND); repository.deleteById(id); }
}
