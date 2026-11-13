package br.com.zonageo.service;

import br.com.zonageo.dto.AdminDtos.PontoRequest;
import br.com.zonageo.model.PontoApoio;
import br.com.zonageo.repository.PontoApoioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class PontoService {
    private final PontoApoioRepository pontos;

    public PontoService(PontoApoioRepository pontos) {
        this.pontos = pontos;
    }

    @Transactional(readOnly = true)
    public List<PontoApoio> listar() {
        return pontos.findAllByOrderByNomeAsc();
    }

    @Transactional
    public PontoApoio criar(PontoRequest req) {
        return pontos.save(aplicar(new PontoApoio(), req));
    }

    @Transactional
    public PontoApoio atualizar(Long id, PontoRequest req) {
        return pontos.save(aplicar(buscar(id), req));
    }

    /** Desativa em vez de apagar, para preservar o histórico de vouchers. */
    @Transactional
    public PontoApoio alternarAtivo(Long id, boolean ativo) {
        PontoApoio p = buscar(id);
        p.setAtivo(ativo);
        return pontos.save(p);
    }

    private PontoApoio buscar(Long id) {
        return pontos.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Ponto não encontrado."));
    }

    private static PontoApoio aplicar(PontoApoio p, PontoRequest r) {
        p.setNome(r.nome().trim());
        p.setTipo(r.tipo());
        p.setLatitude(r.latitude());
        p.setLongitude(r.longitude());
        p.setDescricao(vazioParaNulo(r.descricao()));
        p.setEndereco(vazioParaNulo(r.endereco()));
        p.setHorario(vazioParaNulo(r.horario()));
        p.setGratuito(r.gratuito());
        p.setSombra(r.sombra());
        return p;
    }

    private static String vazioParaNulo(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
