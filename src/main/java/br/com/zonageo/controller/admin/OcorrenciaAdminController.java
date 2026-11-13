package br.com.zonageo.controller.admin;

import br.com.zonageo.model.Ocorrencia;
import br.com.zonageo.repository.OcorrenciaRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/ocorrencias")
public class OcorrenciaAdminController {
    private static final List<String> STATUS_VALIDOS =
            List.of("Aberto", "Em análise", "Validado", "Em atendimento", "Resolvido", "Falso positivo", "Cancelado");

    private final OcorrenciaRepository ocorrencias;

    public OcorrenciaAdminController(OcorrenciaRepository ocorrencias) {
        this.ocorrencias = ocorrencias;
    }

    @GetMapping
    public List<Ocorrencia> listar() {
        return ocorrencias.findAll();
    }

    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public Ocorrencia criar(@Valid @RequestBody Ocorrencia item) {
        if ((item.getLatitude() == null) != (item.getLongitude() == null)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe latitude e longitude juntas.");
        }
        if (item.getSeveridade() == null || item.getSeveridade().isBlank()) item.setSeveridade("Moderado");
        if (item.getStatus() == null || item.getStatus().isBlank()) item.setStatus("Aberto");
        return ocorrencias.save(item);
    }

    @PatchMapping("/{id}/status")
    public Ocorrencia atualizarStatus(@PathVariable Long id, @RequestBody StatusRequest req) {
        if (req.status() == null || !STATUS_VALIDOS.contains(req.status())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status inválido.");
        }
        Ocorrencia item = ocorrencias.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Ocorrência não encontrada."));
        item.setStatus(req.status());
        return ocorrencias.save(item);
    }

    public record StatusRequest(String status) {}
}
