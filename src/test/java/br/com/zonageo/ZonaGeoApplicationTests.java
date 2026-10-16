package br.com.zonageo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;

/** Teste de integração: precisa do PostgreSQL/PostGIS no ar. Rode com ZONAGEO_TEST_DB=true. */
@SpringBootTest(properties = "zonageo.jwt.secret=segredo-somente-para-testes-1234567890abcdef")
@EnabledIfEnvironmentVariable(named = "ZONAGEO_TEST_DB", matches = "true")
class ZonaGeoApplicationTests {
    @Test void contextoInicia() { }
}
