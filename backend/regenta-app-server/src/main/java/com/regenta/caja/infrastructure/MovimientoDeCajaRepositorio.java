package com.regenta.caja.infrastructure;

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

    java.util.Optional<MovimientoDeCaja> findByIdAndNegocioId(UUID id, UUID negocioId);

    boolean existsByAnulaA(UUID movimientoId);

    /** HU-143 C4 · La anulación de un movimiento, si la tiene. */
    java.util.Optional<MovimientoDeCaja> findFirstByAnulaA(UUID movimientoId);

    /** HU-143 C5 · Todos los movimientos de un mismo cobro: uno por método. */
    List<MovimientoDeCaja> findByNegocioIdAndOrigenTipoAndOrigenIdOrderByOcurridoEnAsc(UUID negocioId,
            String origenTipo, UUID origenId);

    /** HU-172 · La salida de un pago a proveedor, por su llave ({@code PAGO_PROVEEDOR:<pagoId>}). */
    java.util.Optional<MovimientoDeCaja> findByNegocioIdAndIdempotencyKey(UUID negocioId, String idempotencyKey);

    /** El efectivo esperado de una sesión: suma firmada de los movimientos en efectivo. */
    @Query("""
            select coalesce(sum(m.monto * m.signo), 0)
            from MovimientoDeCaja m
            where m.sesionId = :sesionId
              and m.metodoPago = com.regenta.caja.domain.MetodoPagoCaja.EFECTIVO
            """)
    BigDecimal efectivoEsperado(@Param("sesionId") UUID sesionId);

    /** Totales de una sesión agrupados por método de pago (HU-063 criterio 1). */
    @Query("""
            select new com.regenta.caja.application.TotalPorMetodo(
                m.metodoPago, sum(m.monto * m.signo), sum(m.propina), count(m))
            from MovimientoDeCaja m
            where m.sesionId = :sesionId
            group by m.metodoPago
            order by m.metodoPago
            """)
    List<com.regenta.caja.application.TotalPorMetodo> totalesPorMetodo(
            @Param("sesionId") UUID sesionId);
}
