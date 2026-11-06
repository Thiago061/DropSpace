package br.com.zonageo.service;

import br.com.zonageo.model.TipoPonto;
import br.com.zonageo.service.RecomendacaoEngine.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RecomendacaoEngineTest {
    private final PontoProximo agua = new PontoProximo(1L, "Bebedouro da praça", TipoPonto.PONTO_AGUA, 350, true, false, null, 0, 0);
    private final PontoProximo sombra = new PontoProximo(2L, "Parque", TipoPonto.DESCANSO_SOMBRA, 900, true, true, null, 0, 0);
    private final PontoProximo mercado = new PontoProximo(3L, "Mercearia do Seu Zé", TipoPonto.MERCADO_PARCEIRO, 1200, false, false, "1 água 500 ml", 0, 0);
    private final PontoProximo abrigo = new PontoProximo(4L, "Abrigo central", TipoPonto.ABRIGO_FRIO, 2000, true, false, null, 0, 0);
    private final List<PontoProximo> pontos = List.of(agua, sombra, mercado, abrigo);

    private static boolean tem(Avaliacao a, String categoria) {
        return a.recomendacoes().stream().anyMatch(r -> r.categoria().equals(categoria));
    }

    @Test void classificaFaixasTermicas() {
        assertEquals(NivelTermico.EXTREMO, RecomendacaoEngine.classificar(41));
        assertEquals(NivelTermico.MUITO_QUENTE, RecomendacaoEngine.classificar(36));
        assertEquals(NivelTermico.QUENTE, RecomendacaoEngine.classificar(30));
        assertEquals(NivelTermico.AGRADAVEL, RecomendacaoEngine.classificar(22));
        assertEquals(NivelTermico.FRIO, RecomendacaoEngine.classificar(10));
        assertEquals(NivelTermico.MUITO_FRIO, RecomendacaoEngine.classificar(3));
    }

    @Test void calorExtremoIndicaAguaSombraEColocaAlertaPrimeiro() {
        Avaliacao a = RecomendacaoEngine.avaliar(new Clima(36, 41, 25.0, 9.0, null, "t"), List.of(), pontos, false);
        assertEquals(NivelTermico.EXTREMO, a.nivel());
        assertEquals(Prioridade.ALERTA, a.recomendacoes().get(0).prioridade());
        assertTrue(a.recomendacoes().stream().anyMatch(r -> r.categoria().equals("agua")
                && r.texto().contains("Bebedouro da praça") && r.texto().contains("350 m")));
        assertTrue(tem(a, "sombra"));
        assertFalse(tem(a, "abrigo"));
    }

    @Test void vouchersDependemDoPlano() {
        Clima calor = new Clima(36, 41, 25.0, 9.0, null, "t");
        Avaliacao free = RecomendacaoEngine.avaliar(calor, List.of(), pontos, false);
        Avaliacao pro = RecomendacaoEngine.avaliar(calor, List.of(), pontos, true);
        assertTrue(free.recomendacoes().stream().anyMatch(r -> r.categoria().equals("parceiro") && r.texto().contains("plano Pro")));
        assertTrue(pro.recomendacoes().stream().anyMatch(r -> r.categoria().equals("parceiro") && r.texto().contains("Gere seu voucher")));
    }

    @Test void frioIndicaAbrigoENaoAgua() {
        Avaliacao a = RecomendacaoEngine.avaliar(new Clima(4, 2, 70.0, 1.0, null, "t"), List.of(), pontos, false);
        assertEquals(NivelTermico.MUITO_FRIO, a.nivel());
        assertTrue(tem(a, "abrigo"));
        assertFalse(tem(a, "agua"));
    }

    @Test void queimadaCriticaGeraAlertaComTelefoneEDistancia() {
        OcorrenciaProxima oc = new OcorrenciaProxima(9L, "Foco", "Queimada", "Crítico", "Aberto", 1500, 0.0, 0.0);
        Avaliacao a = RecomendacaoEngine.avaliar(new Clima(24, 24, 55.0, 3.0, null, "t"), List.of(oc), pontos, false);
        assertEquals(1, a.recomendacoes().size());
        assertEquals(Prioridade.ALERTA, a.recomendacoes().get(0).prioridade());
        assertTrue(a.recomendacoes().get(0).texto().contains("193"));
        assertTrue(a.recomendacoes().get(0).texto().contains("1,5 km"));
    }

    @Test void arSecoCriticoAlertaEIndicaAgua() {
        Avaliacao a = RecomendacaoEngine.avaliar(new Clima(29, 29, 18.0, 5.0, null, "t"), List.of(), pontos, false);
        assertTrue(a.recomendacoes().stream().anyMatch(r -> r.categoria().equals("umidade") && r.prioridade() == Prioridade.ALERTA));
        assertTrue(tem(a, "agua"));
    }

    @Test void semClimaOuSemPontosNaoQuebra() {
        Avaliacao semClima = RecomendacaoEngine.avaliar(null, List.of(), List.of(), false);
        assertNull(semClima.nivel());
        assertEquals(1, semClima.recomendacoes().size());
        Avaliacao semPontos = RecomendacaoEngine.avaliar(new Clima(36, 41, 25.0, 9.0, null, "t"), List.of(), List.of(), false);
        assertTrue(tem(semPontos, "pontos"));
    }
}
