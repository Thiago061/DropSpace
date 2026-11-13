package br.com.zonageo.repository;

import br.com.zonageo.model.OfertaVoucher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface OfertaVoucherRepository extends JpaRepository<OfertaVoucher, Long> {
    List<OfertaVoucher> findAllByOrderByIdDesc();

    /** Ofertas ativas dos pontos informados, da mais antiga para a mais nova. */
    @Query("select o from OfertaVoucher o where o.pontoId in :ids and o.ativo = true "
            + "order by o.id")
    List<OfertaVoucher> vigentesDosPontos(@Param("ids") Collection<Long> ids);
}
