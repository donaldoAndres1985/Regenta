package com.regenta.facturacion.infra;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.regenta.facturacion.domain.EstadoFactura;
import com.regenta.facturacion.domain.Factura;
import com.regenta.facturacion.domain.OrigenDeFactura;
import com.regenta.facturacion.domain.TipoDocumento;

public interface FacturaRepositorio extends JpaRepository<Factura, UUID> {

    List<Factura> findByNegocioIdOrderByFechaEmisionDesc(UUID negocioId);

    Optional<Factura> findByIdAndNegocioId(UUID id, UUID negocioId);

    /** Criterio 4 de HU-053: ya hay una factura para este documento de origen. */
    boolean existsByNegocioIdAndOrigenTipoAndOrigenIdAndEstadoNot(UUID negocioId,
            OrigenDeFactura origenTipo, UUID origenId, EstadoFactura estado);

    /** La factura de venta que salió de este documento de origen (para la NC). */
    Optional<Factura> findFirstByNegocioIdAndOrigenTipoAndOrigenId(UUID negocioId,
            OrigenDeFactura origenTipo, UUID origenId);

    /** Las notas crédito enlazadas a una factura (HU-056 criterio 4). */
    List<Factura> findByNegocioIdAndFacturaOrigenIdOrderByFechaEmisionAsc(UUID negocioId,
            UUID facturaOrigenId);

    /** Ya se emitió una NC para esta devolución (HU-056 criterio 3, idempotencia). */
    boolean existsByNegocioIdAndTipoDocumentoAndOrigenTipoAndOrigenIdAndEstadoNot(UUID negocioId,
            TipoDocumento tipoDocumento, OrigenDeFactura origenTipo, UUID origenId,
            EstadoFactura estado);
}
