package br.com.zonageo.geo;

/** Utilitários geográficos puros (sem dependência de framework). */
public final class Geo {
    private static final double RAIO_TERRA_M = 6_371_008.8;

    private Geo() {}

    /** Distância entre dois pontos (fórmula de haversine), em metros. */
    public static double distanciaM(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * RAIO_TERRA_M * Math.asin(Math.min(1.0, Math.sqrt(a)));
    }

    /**
     * Resultado da projeção de um ponto sobre o trecho A→B.
     * @param distanciaAoTrechoM menor distância do ponto ao trecho, em metros
     * @param progresso posição do ponto projetado ao longo do trecho (0 = A, 1 = B)
     */
    public record Projecao(double distanciaAoTrechoM, double progresso) {}

    /** Projeção plana local (adequada para trechos de poucos km). */
    public static Projecao projetar(double pLat, double pLon, double aLat, double aLon, double bLat, double bLon) {
        double cosLat = Math.cos(Math.toRadians(aLat));
        double px = Math.toRadians(pLon - aLon) * cosLat * RAIO_TERRA_M;
        double py = Math.toRadians(pLat - aLat) * RAIO_TERRA_M;
        double bx = Math.toRadians(bLon - aLon) * cosLat * RAIO_TERRA_M;
        double by = Math.toRadians(bLat - aLat) * RAIO_TERRA_M;
        double len2 = bx * bx + by * by;
        double t = len2 == 0 ? 0 : Math.max(0, Math.min(1, (px * bx + py * by) / len2));
        return new Projecao(Math.hypot(px - t * bx, py - t * by), t);
    }
}
