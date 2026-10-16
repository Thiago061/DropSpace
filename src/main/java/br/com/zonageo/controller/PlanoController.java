package br.com.zonageo.controller;

import br.com.zonageo.config.ZonaGeoProperties;
import br.com.zonageo.dto.AppDtos.PlanoInfo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

/** Catálogo público de planos: fonte única para a página de vendas e para o app. */
@RestController
@RequestMapping("/api/planos")
public class PlanoController {
    private final ZonaGeoProperties props;

    public PlanoController(ZonaGeoProperties props) { this.props = props; }

    @GetMapping
    public List<PlanoInfo> listar() {
        return List.of(
                new PlanoInfo("FREE", "Gratuito", BigDecimal.ZERO, List.of(
                        "Clima e sensação térmica da sua localização",
                        "Ocorrências registradas perto de você",
                        "Recomendações para calor e frio",
                        "Mapa de pontos de água, descanso e abrigo")),
                new PlanoInfo("PRO", "Pro", props.planos().proPrecoMensal(), List.of(
                        "Tudo do plano Gratuito",
                        "Rotas com paradas de água, sombra e abrigo",
                        "Aviso de ocorrências perto do seu trajeto",
                        "Vouchers em mercados parceiros (ex.: água grátis)")));
    }
}
