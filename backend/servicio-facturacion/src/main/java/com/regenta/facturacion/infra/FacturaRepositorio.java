package com.regenta.facturacion.infra;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.regenta.facturacion.domain.EstadoFactura;
import com.regenta.facturacion.domain.Factura;
import com.regenta.facturacion.domain.OrigenDeFactura;

public interface FacturaRepositorio extends JpaRepository<Factura, UUID> {

    List<Factura> findByNegocioIdOrderByFechaEmisionDesc(UUID negocioId);

    Optional<Factura> findByIdAndNegocioId(UUID id, UUID negocioId);

    /** Criterio 4: ya hay una factura para este documento de origen. */
    boolean existsByNegocioIdAndOrigenTipoAndOrigenIdAndEstadoNot(UUID negocioId,
            OrigenDeFactura origenTipo, UUID origenId, EstadoFactura estado);
}
