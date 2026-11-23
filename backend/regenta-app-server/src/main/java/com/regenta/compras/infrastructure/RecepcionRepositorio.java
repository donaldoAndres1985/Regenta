package com.regenta.compras.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.regenta.compras.domain.Recepcion;

public interface RecepcionRepositorio extends JpaRepository<Recepcion, UUID> {

    Optional<Recepcion> findByIdAndNegocioId(UUID id, UUID negocioId);

    /** HU-182 · I32: la que ya llegó desde el celular con ese identificador. */
    Optional<Recepcion> findByNegocioIdAndIdDispositivo(UUID negocioId, UUID idDispositivo);

    List<Recepcion> findByNegocioIdOrderByFechaDesc(UUID negocioId);

    List<Recepcion> findByNegocioIdAndOrdenIdOrderByFechaDesc(UUID negocioId, UUID ordenId);

    /** HU-167 · Las recepciones de un proveedor con ese número de factura (C6: no se repite). */
    List<Recepcion> findByNegocioIdAndProveedorIdAndFacturaProveedor(UUID negocioId,
            UUID proveedorId, String facturaProveedor);

    /** HU-183 · Las recepciones en un estado dentro de un periodo [desde, hasta). */
    List<Recepcion> findByNegocioIdAndEstadoAndFechaGreaterThanEqualAndFechaLessThan(UUID negocioId,
            com.regenta.compras.domain.EstadoRecepcion estado, java.time.OffsetDateTime desde,
            java.time.OffsetDateTime hasta);
}
