package br.com.zonageo.repository;

import br.com.zonageo.model.PontoApoio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PontoApoioRepository extends JpaRepository<PontoApoio, Long> {
    List<PontoApoio> findAllByOrderByNomeAsc();
    long countByAtivoTrue();

    /** Pontos ativos dentro do raio (metros), do mais próximo ao mais distante. Usa PostGIS. */
    @Query(value = """
            select p.id, p.nome, p.tipo, p.latitude, p.longitude, p.descricao, p.endereco, p.horario,
                   p.gratuito, p.sombra, p.ativo, p.criado_em
            from pontos_apoio p
            where p.ativo = true
              and ST_DWithin(p.ponto, CAST(ST_SetSRID(ST_MakePoint(:lon, :lat), 4326) AS geography), :raioM)
            order by ST_Distance(p.ponto, CAST(ST_SetSRID(ST_MakePoint(:lon, :lat), 4326) AS geography))
            limit 60
            """, nativeQuery = true)
    List<PontoApoio> proximos(@Param("lat") double lat, @Param("lon") double lon, @Param("raioM") double raioM);
}
