package br.com.zonageo.repository;

import br.com.zonageo.model.Acesso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AcessoRepository extends JpaRepository<Acesso, Long> {
    List<Acesso> findTop100ByOrderByCriadoEmDesc();
}
