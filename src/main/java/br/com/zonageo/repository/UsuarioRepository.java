package br.com.zonageo.repository;

import br.com.zonageo.model.Papel;
import br.com.zonageo.model.Plano;
import br.com.zonageo.model.Usuario;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByLogin(String login);
    boolean existsByLogin(String login);
    List<Usuario> findTop200ByPapelOrderByCriadoEmDesc(Papel papel);
    long countByPapel(Papel papel);
    long countByPapelAndPlano(Papel papel, Plano plano);

    /** Trava a linha do usuário para serializar emissões de voucher concorrentes. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from Usuario u where u.id = :id")
    Optional<Usuario> buscarComTrava(@Param("id") Long id);
}
