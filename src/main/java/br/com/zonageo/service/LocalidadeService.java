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
import java.util.Map;

@Service
public class LocalidadeService {
    private final ClimaService clima;
    private final OcorrenciaRepository ocorrencias;
    private final PontoApoioRepository pontos;
    private final OfertaService ofertas;

    public LocalidadeService(ClimaService clima, OcorrenciaRepository ocorrencias,
                             PontoApoioRepository pontos, OfertaService ofertas) {
        this.clima = clima;
        this.ocorrencias = ocorrencias;
        this.pontos = pontos;
        this.ofertas = ofertas;
    }

    @Transactional(readOnly = true)
    public LocalidadeResposta consultar(Usuario usuario, double lat, double lon, int raioM) {
        Clima c = clima.obter(lat, lon).orElse(null);

        List<OcorrenciaProxima> proximas = ocorrencias.proximas(lat, lon, raioM).stream()
                .map(o -> paraOcorrencia(o, lat, lon))
                .toList();
        List<PontoApoio> proximosPontos = pontos.proximos(lat, lon, raioM);
        Map<Long, String> vouchers = ofertas.tituloPrincipalPorPonto(
                proximosPontos.stream().map(PontoApoio::getId).toList());
        List<PontoProximo> apoio = proximosPontos.stream()
                .map(p -> paraPonto(p, lat, lon, vouchers.get(p.getId())))
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

    private static PontoProximo paraPonto(PontoApoio p, double lat, double lon, String voucher) {
        return new PontoProximo(p.getId(), p.getNome(), p.getTipo(),
                Geo.distanciaM(lat, lon, p.getLatitude(), p.getLongitude()), p.isGratuito(), p.isSombra(),
                voucher, p.getLatitude(), p.getLongitude());
    }
}
