package br.com.zonageo.service;

import br.com.zonageo.dto.AppDtos;
import br.com.zonageo.dto.AppDtos.LocalidadeResposta;
import br.com.zonageo.geo.Geo;
import br.com.zonageo.model.Ocorrencia;
import br.com.zonageo.model.PontoApoio;
import br.com.zonageo.model.Usuario;
import br.com.zonageo.repository.OcorrenciaRepository;
import br.com.zonageo.repository.PontoApoioRepository;
import br.com.zonageo.service.RecomendacaoEngine.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class LocalidadeService {
    private final ClimaService clima;
    private final OcorrenciaRepository ocorrencias;
    private final PontoApoioRepository pontos;

    public LocalidadeService(ClimaService clima, OcorrenciaRepository ocorrencias, PontoApoioRepository pontos) {
        this.clima = clima;
        this.ocorrencias = ocorrencias;
        this.pontos = pontos;
    }

    @Transactional(readOnly = true)
    public LocalidadeResposta consultar(Usuario usuario, double lat, double lon, int raioM) {
        Clima c = clima.obter(lat, lon).orElse(null);

        List<OcorrenciaProxima> proximas = ocorrencias.proximas(lat, lon, raioM).stream()
                .map(o -> paraOcorrencia(o, lat, lon))
                .toList();
        List<PontoProximo> apoio = pontos.proximos(lat, lon, raioM).stream()
                .map(p -> paraPonto(p, lat, lon))
                .toList();

        boolean pro = usuario.isPro();
        Avaliacao av = RecomendacaoEngine.avaliar(c, proximas, apoio, pro);
        return new LocalidadeResposta(lat, lon, raioM, usuario.planoEfetivo().name(), c,
                av.nivel() == null ? null : av.nivel().name(),
                av.nivel() == null ? null : av.nivel().rotulo(),
                av.recomendacoes(), proximas, apoio, AppDtos.AVISO);
    }

    private static OcorrenciaProxima paraOcorrencia(Ocorrencia o, double lat, double lon) {
        return new OcorrenciaProxima(o.getId(), o.getTitulo(), o.getCategoria(), o.getSeveridade(), o.getStatus(),
                Geo.distanciaM(lat, lon, o.getLatitude(), o.getLongitude()), o.getLatitude(), o.getLongitude());
    }

    private static PontoProximo paraPonto(PontoApoio p, double lat, double lon) {
        return new PontoProximo(p.getId(), p.getNome(), p.getTipo(),
                Geo.distanciaM(lat, lon, p.getLatitude(), p.getLongitude()), p.isGratuito(), p.isSombra(),
                p.oferecaVoucher() ? p.getVoucherDescricao() : null, p.getLatitude(), p.getLongitude());
    }
}
