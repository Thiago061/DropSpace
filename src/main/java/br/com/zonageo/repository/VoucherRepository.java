package br.com.zonageo.repository;

import br.com.zonageo.model.StatusVoucher;
import br.com.zonageo.model.Voucher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface VoucherRepository extends JpaRepository<Voucher, Long> {
    Optional<Voucher> findByCodigo(String codigo);
    List<Voucher> findTop50ByUsuarioIdOrderByEmitidoEmDesc(Long usuarioId);
    List<Voucher> findTop100ByOrderByEmitidoEmDesc();
    long countByUsuarioIdAndEmitidoEmGreaterThanEqual(Long usuarioId, Instant desde);
    Optional<Voucher> findFirstByUsuarioIdAndPontoIdAndStatusAndExpiraEmAfter(
            Long usuarioId, Long pontoId, StatusVoucher status, Instant agora);

    /** Resgate atômico: só tem efeito se o voucher estiver ativo e dentro da validade. */
    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("update Voucher v set v.status = br.com.zonageo.model.StatusVoucher.RESGATADO, v.resgatadoEm = :agora "
            + "where v.codigo = :codigo and v.status = br.com.zonageo.model.StatusVoucher.ATIVO and v.expiraEm > :agora")
    int resgatar(@Param("codigo") String codigo, @Param("agora") Instant agora);
}
