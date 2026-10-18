package br.com.zonageo.repository;

import br.com.zonageo.model.Papel;
import br.com.zonageo.model.Plano;
import br.com.zonageo.model.Usuario;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByLogin(String login);
    boolean existsByLogin(String login);
    Page<Usuario> findByNomeContainingIgnoreCaseOrLoginContainingIgnoreCase(String nome, String login, Pageable pagina);
    long countByCriadoEmGreaterThanEqual(Instant desde);
    long countByUltimoLoginEmGreaterThanEqual(Instant desde);
    long countByAtivoFalse();
    long countByPapel(Papel papel);
    long countByPapelAndPlano(Papel papel, Plano plano);

    /** Assinantes com Pro realmente vigente (sem contar planos vencidos). */
    @Query("select count(u) from Usuario u where u.plano = br.com.zonageo.model.Plano.PRO "
            + "and (u.planoExpiraEm is null or u.planoExpiraEm > :agora)")
    long contarProVigentes(@Param("agora") Instant agora);

    /** Trava a linha do usuário para serializar emissões de voucher concorrentes. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from Usuario u where u.id = :id")
    Optional<Usuario> buscarComTrava(@Param("id") Long id);
}
