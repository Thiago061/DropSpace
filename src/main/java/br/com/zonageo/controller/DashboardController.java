package br.com.zonageo.controller;

import br.com.zonageo.model.Papel;
import br.com.zonageo.model.Plano;
import br.com.zonageo.repository.LocalizacaoRepository;
import br.com.zonageo.repository.MedicaoAmbientalRepository;
import br.com.zonageo.repository.OcorrenciaRepository;
import br.com.zonageo.repository.PontoApoioRepository;
import br.com.zonageo.repository.UsuarioRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    private static final List<String> ENCERRADOS = List.of("Resolvido", "Cancelado", "Falso positivo");

    private final LocalizacaoRepository localizacoes;
    private final MedicaoAmbientalRepository medicoes;
    private final OcorrenciaRepository ocorrencias;
    private final PontoApoioRepository pontos;
    private final UsuarioRepository usuarios;

    public DashboardController(LocalizacaoRepository localizacoes, MedicaoAmbientalRepository medicoes,
                               OcorrenciaRepository ocorrencias, PontoApoioRepository pontos,
                               UsuarioRepository usuarios) {
        this.localizacoes = localizacoes;
        this.medicoes = medicoes;
        this.ocorrencias = ocorrencias;
        this.pontos = pontos;
        this.usuarios = usuarios;
    }

    @GetMapping
    public Map<String, Long> indicadores() {
        return Map.of(
                "localizacoes", localizacoes.count(),
                "medicoes", medicoes.count(),
                "ocorrencias", ocorrencias.count(),
                "ocorrenciasAbertas", ocorrencias.countByStatusNotIn(ENCERRADOS),
                "pontosApoio", pontos.countByAtivoTrue(),
                "usuarios", usuarios.countByPapel(Papel.USER),
                "usuariosPro", usuarios.countByPapelAndPlano(Papel.USER, Plano.PRO));
    }
}
