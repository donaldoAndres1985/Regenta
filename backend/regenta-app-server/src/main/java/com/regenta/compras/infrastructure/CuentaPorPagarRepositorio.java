package com.regenta.compras.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.regenta.compras.domain.CuentaPorPagar;

public interface CuentaPorPagarRepositorio extends JpaRepository<CuentaPorPagar, UUID> {

    List<CuentaPorPagar> findByNegocioIdAndRecepcionId(UUID negocioId, UUID recepcionId);

    Optional<CuentaPorPagar> findByIdAndNegocioId(UUID id, UUID negocioId);

    /** Criterio 3: por antigÃ¼edad = vencimiento mÃ¡s viejo primero. */
    List<CuentaPorPagar> findByNegocioIdOrderByFechaVencimientoAscFechaEmisionAsc(UUID negocioId);

    /** HU-171 · Las cuentas de un proveedor, la que vence primero al principio. */
    List<CuentaPorPagar> findByNegocioIdAndProveedorIdOrderByFechaVencimientoAscFechaEmisionAsc(
            UUID negocioId, UUID proveedorId);

    boolean existsByNegocioIdAndProveedorIdAndNumeroFactura(UUID negocioId, UUID proveedorId,
            String numeroFactura);

    /**
     * HU-166 · Lo que se le debe a cada proveedor: la suma de los saldos de sus cuentas abiertas
     * (I5). Cada fila es {@code [proveedorId, saldo]}; un proveedor sin cuentas abiertas no aparece.
     */
    @Query("""
            select c.proveedorId, sum(c.saldo)
            from CuentaPorPagar c
            where c.negocioId = :negocioId
              and c.estado <> com.regenta.compras.domain.EstadoCuentaPorPagar.PAGADA
              and c.estado <> com.regenta.compras.domain.EstadoCuentaPorPagar.ANULADA
            group by c.proveedorId
            """)
    List<Object[]> saldosAbiertosPorProveedor(@Param("negocioId") UUID negocioId);
}
