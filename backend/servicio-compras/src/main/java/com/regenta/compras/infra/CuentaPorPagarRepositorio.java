package com.regenta.compras.infra;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.regenta.compras.domain.CuentaPorPagar;

public interface CuentaPorPagarRepositorio extends JpaRepository<CuentaPorPagar, UUID> {

    List<CuentaPorPagar> findByNegocioIdAndRecepcionId(UUID negocioId, UUID recepcionId);

    Optional<CuentaPorPagar> findByIdAndNegocioId(UUID id, UUID negocioId);

    /** Criterio 3: por antigüedad = vencimiento más viejo primero. */
    List<CuentaPorPagar> findByNegocioIdOrderByFechaVencimientoAscFechaEmisionAsc(UUID negocioId);

    boolean existsByNegocioIdAndProveedorIdAndNumeroFactura(UUID negocioId, UUID proveedorId,
            String numeroFactura);
}
