package br.com.zonageo.repository;
import br.com.zonageo.model.Localizacao;
import org.springframework.data.jpa.repository.JpaRepository;
public interface LocalizacaoRepository extends JpaRepository<Localizacao, Long> {}
