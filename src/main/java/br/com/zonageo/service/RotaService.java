package br.com.zonageo.service;

import br.com.zonageo.dto.AppDtos.AlertaRota;
import br.com.zonageo.dto.AppDtos.Parada;
import br.com.zonageo.dto.AppDtos.RotaResposta;
import br.com.zonageo.geo.Geo;
import br.com.zonageo.model.Ocorrencia;
import br.com.zonageo.model.PontoApoio;
import br.com.zonageo.model.TipoPonto;
import br.com.zonageo.repository.OcorrenciaRepository;
import br.com.zonageo.repository.PontoApoioRepository;
import br.com.zonageo.service.RecomendacaoEngine.NivelTermico;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Rota Pro: escolhe paradas de apoio (água, sombra, abrigo, mercados parceiros) próximas ao caminho
 * direto entre origem e destino, de acordo com o clima, e avisa sobre ocorrências perto do trajeto.
 *
 * <p>Limitação consciente: não calcula o caminho pelas ruas nem mede sombra real. A qualidade depende
 * dos pontos cadastrados. O link gerado abre o app de mapas do usuário para a navegação em si.
 */
@Service
public class RotaService {
    public static final double DISTANCIA_MAX_M = 20_000;
    private static final double CORREDOR_M = 400;
    private static final double ALERTA_M = 300;
    private static final double ESPACAMENTO_M = 300;
    private static final int MAX_PARADAS = 6;

    private final ClimaService clima;
    private final PontoApoioRepository pontos;
    private final OcorrenciaRepository ocorrencias;
    private final OfertaService ofertas;

    public RotaService(ClimaService clima, PontoApoioRepository pontos,
                       OcorrenciaRepository ocorrencias, OfertaService ofertas) {
        this.clima = clima;
        this.pontos = pontos;
        this.ocorrencias = ocorrencias;
        this.ofertas = ofertas;
    }

    public static boolean distanciaValida(double oLat, double oLon, double dLat, double dLon) {
        return Geo.distanciaM(oLat, oLon, dLat, dLon) <= DISTANCIA_MAX_M;
    }

    @Transactional(readOnly = true)
    public RotaResposta montar(double oLat, double oLon, double dLat, double dLon) {
        double direta = Geo.distanciaM(oLat, oLon, dLat, dLon);
        double meioLat = (oLat + dLat) / 2;
        double meioLon = (oLon + dLon) / 2;
        double raioBusca = direta / 2 + CORREDOR_M;

        String modo = definirModo(oLat, oLon);
        Set<TipoPonto> relevantes = switch (modo) {
            case "CALOR" -> Set.of(TipoPonto.PONTO_AGUA, TipoPonto.DESCANSO_SOMBRA, TipoPonto.MERCADO_PARCEIRO);
            case "FRIO" -> Set.of(TipoPonto.ABRIGO_FRIO, TipoPonto.MERCADO_PARCEIRO);
            default -> Set.of(TipoPonto.PONTO_AGUA, TipoPonto.MERCADO_PARCEIRO);
        };

        List<Candidato> candidatos = new ArrayList<>();
        for (PontoApoio p : pontos.proximos(meioLat, meioLon, raioBusca)) {
            if (!relevantes.contains(p.getTipo())) continue;
            Geo.Projecao pr = Geo.projetar(p.getLatitude(), p.getLongitude(), oLat, oLon, dLat, dLon);
            if (pr.distanciaAoTrechoM() <= CORREDOR_M) candidatos.add(new Candidato(p, pr));
        }
        // pontos com sombra, e mais próximos do caminho, primeiro
        candidatos.sort(Comparator
                .comparing((Candidato c) -> !(modo.equals("CALOR") && c.ponto.isSombra()))
                .thenComparingDouble((Candidato c) -> c.proj.distanciaAoTrechoM()));

        List<Candidato> escolhidos = new ArrayList<>();
        for (Candidato c : candidatos) {
            if (escolhidos.size() >= MAX_PARADAS) break;
            boolean longeDosOutros = escolhidos.stream().allMatch(e ->
                    Math.abs(e.proj.progresso() - c.proj.progresso()) * direta >= ESPACAMENTO_M);
            if (longeDosOutros) escolhidos.add(c);
        }
        escolhidos.sort(Comparator.comparingDouble((Candidato c) -> c.proj.progresso()));

        Map<Long, String> vouchers = ofertas.tituloPrincipalPorPonto(
                escolhidos.stream().map(c -> c.ponto.getId()).toList());
        List<Parada> paradas = escolhidos.stream().map(c -> new Parada(
                c.ponto.getId(), c.ponto.getNome(), c.ponto.getTipo(), c.ponto.getLatitude(), c.ponto.getLongitude(),
                c.proj.progresso() * direta, c.proj.distanciaAoTrechoM(), c.ponto.isSombra(), c.ponto.isGratuito(),
                vouchers.get(c.ponto.getId()))).toList();

        List<AlertaRota> alertas = new ArrayList<>();
        for (Ocorrencia o : ocorrencias.proximas(meioLat, meioLon, direta / 2 + ALERTA_M)) {
            Geo.Projecao pr = Geo.projetar(o.getLatitude(), o.getLongitude(), oLat, oLon, dLat, dLon);
            if (pr.distanciaAoTrechoM() <= ALERTA_M) {
                alertas.add(new AlertaRota(o.getTitulo(), o.getCategoria(), o.getSeveridade(),
                        pr.distanciaAoTrechoM(), "Considere contornar este trecho ou escolher outro horário."));
            }
        }

        return new RotaResposta(direta, modo, paradas, alertas, linkMapa(oLat, oLon, dLat, dLon, paradas),
                "Sugestão com base nos pontos cadastrados perto do caminho direto. Não mede sombra real nem "
                        + "substitui a navegação do seu app de mapas. Confira as condições locais.");
    }

    private String definirModo(double lat, double lon) {
        return clima.obter(lat, lon).map(c -> {
            NivelTermico n = RecomendacaoEngine.classificar(c.sensacaoC());
            if (n.calor()) return "CALOR";
            if (n.frio()) return "FRIO";
            return "NEUTRO";
        }).orElse("NEUTRO");
    }

    static String linkMapa(double oLat, double oLon, double dLat, double dLon, List<Parada> paradas) {
        StringBuilder sb = new StringBuilder("https://www.google.com/maps/dir/?api=1&travelmode=walking");
        sb.append(String.format(Locale.ROOT, "&origin=%.6f,%.6f&destination=%.6f,%.6f", oLat, oLon, dLat, dLon));
        if (!paradas.isEmpty()) {
            sb.append("&waypoints=");
            for (int i = 0; i < paradas.size(); i++) {
                if (i > 0) sb.append("%7C");
                sb.append(String.format(Locale.ROOT, "%.6f,%.6f", paradas.get(i).latitude(), paradas.get(i).longitude()));
            }
        }
        return sb.toString();
    }

    private record Candidato(PontoApoio ponto, Geo.Projecao proj) {}
}
