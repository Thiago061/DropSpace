package br.com.zonageo.controller;
import br.com.zonageo.model.Localizacao;
import br.com.zonageo.repository.LocalizacaoRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
@RestController
@RequestMapping("/api/localizacoes")
public class LocalizacaoController {
    private final LocalizacaoRepository repository;
    public LocalizacaoController(LocalizacaoRepository repository) { this.repository = repository; }
    @GetMapping public List<Localizacao> listar() { return repository.findAll(); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public Localizacao criar(@Valid @RequestBody Localizacao item) { return repository.save(item); }
    @GetMapping("/{id}") public Localizacao buscar(@PathVariable Long id) { return repository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Localização não encontrada")); }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Long id) { if (!repository.existsById(id)) throw new ResponseStatusException(HttpStatus.NOT_FOUND); repository.deleteById(id); }
}
