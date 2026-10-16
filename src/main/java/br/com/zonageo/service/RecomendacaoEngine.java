package br.com.zonageo.service;

import br.com.zonageo.model.TipoPonto;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

/**
 * Motor de recomendações baseado em regras. Java puro, sem framework, para ser fácil de testar.
 *
 * <p>Os limites abaixo são heurísticas de apoio (sensação térmica em °C, umidade relativa em %),
 * não alertas oficiais. Eles ficam centralizados aqui para serem ajustados com validação técnica.
 * No futuro, um modelo de linguagem pode ser plugado por cima, usando estas mesmas entradas.
 */
public final class RecomendacaoEngine {

    public static final double LIMITE_QUENTE = 30;
    public static final double LIMITE_MUITO_QUENTE = 35;
    public static final double LIMITE_EXTREMO = 40;
    public static final double LIMITE_FRIO = 14;
    public static final double LIMITE_MUITO_FRIO = 5;
    public static final double UMIDADE_ATENCAO = 30;
    public static final double UMIDADE_CRITICA = 20;

    private RecomendacaoEngine() {}

    public enum NivelTermico {
        MUITO_FRIO("Muito frio"), FRIO("Frio"), AGRADAVEL("Agradável"),
        QUENTE("Quente"), MUITO_QUENTE("Muito quente"), EXTREMO("Calor extremo");

        private final String rotulo;
        NivelTermico(String rotulo) { this.rotulo = rotulo; }
        public String rotulo() { return rotulo; }
        public boolean calor() { return ordinal() >= QUENTE.ordinal(); }
        public boolean frio() { return ordinal() <= FRIO.ordinal(); }
    }

    public enum Prioridade { INFO, ATENCAO, ALERTA }

    public record Clima(double temperaturaC, double sensacaoC, Double umidade, Double indiceUv,
                        Instant momento, String fonte) {}

    public record Recomendacao(String categoria, Prioridade prioridade, String texto) {}

    public record OcorrenciaProxima(Long id, String titulo, String categoria, String severidade,
                                    String status, double distanciaM, Double latitude, Double longitude) {}

    public record PontoProximo(Long id, String nome, TipoPonto tipo, double distanciaM, boolean gratuito,
                               boolean sombra, String voucher, double latitude, double longitude) {
        public boolean parceiro() { return tipo == TipoPonto.MERCADO_PARCEIRO; }
    }

    public record Avaliacao(NivelTermico nivel, List<Recomendacao> recomendacoes) {}

    public static NivelTermico classificar(double sensacaoC) {
        if (sensacaoC <= LIMITE_MUITO_FRIO) return NivelTermico.MUITO_FRIO;
        if (sensacaoC <= LIMITE_FRIO) return NivelTermico.FRIO;
        if (sensacaoC >= LIMITE_EXTREMO) return NivelTermico.EXTREMO;
        if (sensacaoC >= LIMITE_MUITO_QUENTE) return NivelTermico.MUITO_QUENTE;
        if (sensacaoC >= LIMITE_QUENTE) return NivelTermico.QUENTE;
        return NivelTermico.AGRADAVEL;
    }

    /**
     * @param clima       condições atuais (pode ser {@code null} se o provedor estiver indisponível)
     * @param ocorrencias ocorrências ativas próximas
     * @param pontos      pontos de apoio próximos, já ordenados do mais próximo ao mais distante
     * @param pro         se o usuário tem plano Pro (muda o texto sobre vouchers)
     */
    public static Avaliacao avaliar(Clima clima, List<OcorrenciaProxima> ocorrencias,
                                    List<PontoProximo> pontos, boolean pro) {
        List<Recomendacao> recs = new ArrayList<>();
        NivelTermico nivel = null;
        boolean buscarAgua = false;
        boolean buscarAbrigo = false;

        if (clima == null) {
            recs.add(new Recomendacao("clima", Prioridade.INFO,
                    "Não foi possível obter o clima agora. Mostrando apenas ocorrências e pontos de apoio próximos."));
        } else {
            nivel = classificar(clima.sensacaoC());
            buscarAgua = nivel.calor();
            buscarAbrigo = nivel.frio();
            adicionarTermicas(recs, clima, nivel);

            Double umidade = clima.umidade();
            if (umidade != null) {
                if (umidade < UMIDADE_CRITICA) {
                    buscarAgua = true;
                    recs.add(new Recomendacao("umidade", Prioridade.ALERTA, String.format(Locale.ROOT,
                            "Ar muito seco (umidade %.0f%%). Beba água com frequência, evite esforço físico entre 12h e 16h, "
                                    + "umedeça os ambientes e atenção redobrada para crianças, idosos e quem tem problemas respiratórios.", umidade)));
                } else if (umidade < UMIDADE_ATENCAO) {
                    buscarAgua = true;
                    recs.add(new Recomendacao("umidade", Prioridade.ATENCAO, String.format(Locale.ROOT,
                            "Umidade baixa (%.0f%%). Mantenha-se hidratado e evite exercícios intensos nas horas mais secas do dia.", umidade)));
                }
            }
            Double uv = clima.indiceUv();
            if (uv != null) {
                if (uv >= 11) {
                    recs.add(new Recomendacao("uv", Prioridade.ALERTA, String.format(Locale.ROOT,
                            "Índice UV extremo (%.0f). Evite o sol direto; se precisar sair, use protetor solar, chapéu e óculos com proteção UV.", uv)));
                } else if (uv >= 8) {
                    recs.add(new Recomendacao("uv", Prioridade.ATENCAO, String.format(Locale.ROOT,
                            "Índice UV muito alto (%.0f). Use protetor solar FPS 30 ou mais, chapéu e prefira trajetos com sombra.", uv)));
                } else if (uv >= 6) {
                    recs.add(new Recomendacao("uv", Prioridade.INFO, String.format(Locale.ROOT,
                            "Índice UV alto (%.0f). Protetor solar recomendado.", uv)));
                }
            }
        }

        adicionarOcorrencias(recs, ocorrencias);
        adicionarPontos(recs, pontos, nivel, buscarAgua, buscarAbrigo, pro);

        recs.sort(Comparator.comparingInt((Recomendacao r) -> r.prioridade().ordinal()).reversed());
        return new Avaliacao(nivel, List.copyOf(recs));
    }

    // ---------------------------------------------------------------- térmicas

    private static void adicionarTermicas(List<Recomendacao> recs, Clima c, NivelTermico nivel) {
        String sens = String.format(Locale.ROOT, "%.0f", c.sensacaoC());
        switch (nivel) {
            case QUENTE -> recs.add(new Recomendacao("calor", Prioridade.INFO,
                    "Sensação térmica de " + sens + " °C. Beba água com frequência, mesmo sem sede, e prefira trajetos com sombra."));
            case MUITO_QUENTE -> recs.add(new Recomendacao("calor", Prioridade.ATENCAO,
                    "Sensação térmica de " + sens + " °C. Evite exposição ao sol entre 10h e 16h, faça pausas à sombra, "
                            + "use roupas leves e claras e hidrate-se constantemente."));
            case EXTREMO -> recs.add(new Recomendacao("calor", Prioridade.ALERTA,
                    "Calor extremo: sensação de " + sens + " °C. Risco de desidratação e insolação. Evite atividade ao ar livre, "
                            + "procure um local fresco ou climatizado. Tontura, confusão, náusea ou pele quente e seca exigem "
                            + "atendimento imediato (SAMU 192)."));
            case FRIO -> recs.add(new Recomendacao("frio", Prioridade.INFO,
                    "Sensação térmica de " + sens + " °C. Vista roupas em camadas, proteja mãos, pés e pescoço e prefira bebidas quentes."));
            case MUITO_FRIO -> recs.add(new Recomendacao("frio", Prioridade.ALERTA,
                    "Muito frio: sensação de " + sens + " °C. Há risco de hipotermia, principalmente para idosos, crianças e pessoas "
                            + "em situação de rua. Procure abrigo aquecido, mantenha-se seco e agasalhado e verifique como estão "
                            + "pessoas próximas."));
            default -> { /* AGRADAVEL: sem recomendação térmica */ }
        }
    }

    // -------------------------------------------------------------- ocorrências

    private static int pesoSeveridade(String s) {
        if (s == null) return 0;
        return switch (s) {
            case "Crítico" -> 3;
            case "Alto" -> 2;
            case "Moderado" -> 1;
            default -> 0;
        };
    }

    private static void adicionarOcorrencias(List<Recomendacao> recs, List<OcorrenciaProxima> ocorrencias) {
        if (ocorrencias == null || ocorrencias.isEmpty()) return;
        List<OcorrenciaProxima> principais = ocorrencias.stream()
                .sorted(Comparator.comparingInt((OcorrenciaProxima o) -> pesoSeveridade(o.severidade())).reversed()
                        .thenComparingDouble(OcorrenciaProxima::distanciaM))
                .limit(3)
                .toList();
        for (OcorrenciaProxima o : principais) {
            int peso = pesoSeveridade(o.severidade());
            Prioridade prioridade = peso >= 2 ? Prioridade.ALERTA : peso == 1 ? Prioridade.ATENCAO : Prioridade.INFO;
            recs.add(new Recomendacao("ocorrencia", prioridade, textoOcorrencia(o)));
        }
    }

    private static String textoOcorrencia(OcorrenciaProxima o) {
        String cat = o.categoria() == null ? "" : o.categoria().toLowerCase(Locale.ROOT);
        String onde = "a " + formatarDistancia(o.distanciaM());
        if (cat.contains("queimada")) {
            return "Queimada registrada a " + onde.substring(2) + ". Evite a área, mantenha janelas fechadas por causa da fumaça "
                    + "e, se vir fogo, ligue 193 (Bombeiros).";
        }
        if (cat.contains("inunda") || cat.contains("alag")) {
            return "Risco de alagamento (" + o.titulo() + ") " + onde + ". Evite vias baixas e nunca atravesse água corrente. "
                    + "Defesa Civil: 199.";
        }
        if (cat.contains("tempestade")) {
            return "Tempestade registrada " + onde + ". Abrigue-se em local fechado e afaste-se de árvores, postes e fios.";
        }
        if (cat.contains("calor")) {
            return "Ocorrência de calor extremo registrada " + onde + ". Evite a região nas horas mais quentes e procure sombra e água.";
        }
        if (cat.contains("umidade")) {
            return "Umidade crítica registrada " + onde + ". Hidrate-se e evite esforço físico intenso.";
        }
        return "Ocorrência próxima: " + o.titulo() + " (" + o.severidade() + "), " + onde + ". Redobre a atenção.";
    }

    // ------------------------------------------------------------------ pontos

    private static void adicionarPontos(List<Recomendacao> recs, List<PontoProximo> pontos, NivelTermico nivel,
                                        boolean buscarAgua, boolean buscarAbrigo, boolean pro) {
        if (!buscarAgua && !buscarAbrigo) return;
        if (pontos == null || pontos.isEmpty()) {
            recs.add(new Recomendacao("pontos", Prioridade.INFO,
                    "Nenhum ponto de apoio cadastrado por perto. Tente ampliar o raio da busca."));
            return;
        }
        if (buscarAgua) {
            Prioridade prioridade = nivel != null && nivel.ordinal() >= NivelTermico.MUITO_QUENTE.ordinal()
                    ? Prioridade.ATENCAO : Prioridade.INFO;
            primeiro(pontos, p -> p.tipo() == TipoPonto.PONTO_AGUA).ifPresent(p ->
                    recs.add(new Recomendacao("agua", prioridade,
                            "Ponto de água mais próximo: " + p.nome() + ", a " + formatarDistancia(p.distanciaM())
                                    + (p.gratuito() ? " (gratuito)." : "."))));
            primeiro(pontos, p -> p.tipo() == TipoPonto.DESCANSO_SOMBRA || p.sombra()).ifPresent(p ->
                    recs.add(new Recomendacao("sombra", Prioridade.INFO,
                            "Para descansar na sombra: " + p.nome() + ", a " + formatarDistancia(p.distanciaM()) + ".")));
        }
        if (buscarAbrigo) {
            primeiro(pontos, p -> p.tipo() == TipoPonto.ABRIGO_FRIO).ifPresent(p ->
                    recs.add(new Recomendacao("abrigo", Prioridade.ATENCAO,
                            "Local aquecido mais próximo: " + p.nome() + ", a " + formatarDistancia(p.distanciaM()) + ".")));
        }
        primeiro(pontos, PontoProximo::parceiro).ifPresent(p -> {
            String base = "Mercado parceiro: " + p.nome() + ", a " + formatarDistancia(p.distanciaM()) + ".";
            if (p.voucher() == null || p.voucher().isBlank()) {
                recs.add(new Recomendacao("parceiro", Prioridade.INFO, base));
            } else if (pro) {
                recs.add(new Recomendacao("parceiro", Prioridade.INFO,
                        base + " Gere seu voucher no app e retire: " + p.voucher() + "."));
            } else {
                recs.add(new Recomendacao("parceiro", Prioridade.INFO,
                        base + " Com o plano Pro você troca um voucher por: " + p.voucher() + "."));
            }
        });
    }

    private static java.util.Optional<PontoProximo> primeiro(List<PontoProximo> pontos, Predicate<PontoProximo> filtro) {
        return pontos.stream().filter(filtro).min(Comparator.comparingDouble(PontoProximo::distanciaM));
    }

    public static String formatarDistancia(double metros) {
        if (metros < 1000) {
            return Math.round(metros / 10.0) * 10 + " m";
        }
        return String.format(Locale.forLanguageTag("pt-BR"), "%.1f km", metros / 1000.0);
    }
}
