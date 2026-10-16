package br.com.zonageo.repository;

import br.com.zonageo.model.Ocorrencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface OcorrenciaRepository extends JpaRepository<Ocorrencia, Long> {
    long countByStatusNotIn(Collection<String> status);

    /** Ocorrências ainda ativas (não encerradas) dentro do raio, em metros. */
    @Query(value = """
            select o.id, o.titulo, o.categoria, o.severidade, o.latitude, o.longitude, o.descricao,
                   o.status, o.criada_em, o.atualizada_em
            from ocorrencias o
            where o.ponto is not null
              and o.status not in ('Resolvido', 'Cancelado', 'Falso positivo')
              and ST_DWithin(o.ponto, CAST(ST_SetSRID(ST_MakePoint(:lon, :lat), 4326) AS geography), :raioM)
            order by ST_Distance(o.ponto, CAST(ST_SetSRID(ST_MakePoint(:lon, :lat), 4326) AS geography))
            limit 50
            """, nativeQuery = true)
    List<Ocorrencia> proximas(@Param("lat") double lat, @Param("lon") double lon, @Param("raioM") double raioM);
}
