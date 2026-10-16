package br.com.zonageo.controller;
import br.com.zonageo.model.Ocorrencia;
import br.com.zonageo.repository.OcorrenciaRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
@RestController
@RequestMapping("/api/ocorrencias")
public class OcorrenciaController {
    private final OcorrenciaRepository repository;
    public OcorrenciaController(OcorrenciaRepository repository) { this.repository = repository; }
    @GetMapping public List<Ocorrencia> listar() { return repository.findAll(); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public Ocorrencia criar(@Valid @RequestBody Ocorrencia item) {
        if ((item.getLatitude() == null) != (item.getLongitude() == null)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe latitude e longitude juntas.");
        if (item.getSeveridade() == null || item.getSeveridade().isBlank()) item.setSeveridade("Moderado");
        if (item.getStatus() == null || item.getStatus().isBlank()) item.setStatus("Aberto");
        return repository.save(item);
    }
    @PatchMapping("/{id}/status")
    public Ocorrencia atualizarStatus(@PathVariable Long id, @RequestBody StatusRequest request) {
        if (request.status() == null || !List.of("Aberto", "Em análise", "Validado", "Em atendimento", "Resolvido", "Falso positivo", "Cancelado").contains(request.status())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status inválido.");
        Ocorrencia item = repository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ocorrência não encontrada"));
        item.setStatus(request.status()); return repository.save(item);
    }
    public record StatusRequest(String status) {}
}
