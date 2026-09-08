package com.regenta.caja.infra;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.regenta.caja.domain.MovimientoDeCaja;

public interface MovimientoDeCajaRepositorio extends JpaRepository<MovimientoDeCaja, UUID> {

    List<MovimientoDeCaja> findBySesionId(UUID sesionId);

    List<MovimientoDeCaja> findBySesionIdOrderByOcurridoEnAsc(UUID sesionId);

    boolean existsByNegocioIdAndIdempotencyKey(UUID negocioId, String idempotencyKey);

    /** El efectivo esperado de una sesión: suma firmada de los movimientos en efectivo. */
    @Query("""
            select coalesce(sum(m.monto * m.signo), 0)
            from MovimientoDeCaja m
            where m.sesionId = :sesionId
              and m.metodoPago = com.regenta.caja.domain.MetodoPagoCaja.EFECTIVO
            """)
    BigDecimal efectivoEsperado(@Param("sesionId") UUID sesionId);
}
