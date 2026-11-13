package br.com.zonageo.geo;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeoTest {
    @Test void haversineAproximadamente962Metros() {
        double d = Geo.distanciaM(-15.7939, -47.8828, -15.7939, -47.8738);
        assertEquals(962, d, 15);
    }

    @Test void pontoNoMeioDoTrecho() {
        Geo.Projecao p = Geo.projetar(-15.7939, -47.8783, -15.7939, -47.8828, -15.7939, -47.8738);
        assertTrue(p.distanciaAoTrechoM() < 1);
        assertEquals(0.5, p.progresso(), 0.01);
    }

    @Test void pontoAfastado400Metros() {
        Geo.Projecao p = Geo.projetar(-15.7903, -47.8783, -15.7939, -47.8828, -15.7939, -47.8738);
        assertEquals(400, p.distanciaAoTrechoM(), 10);
    }
}
