package com.regenta.ventas.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.regenta.ventas.domain.EstadoVenta;
import com.regenta.ventas.domain.Venta;

public interface VentaRepositorio extends JpaRepository<Venta, UUID> {

    /** HU-043 criterio 3: la clave con la que una subida repetida se reconoce. */
    Optional<Venta> findByNegocioIdAndOrigenOfflineId(UUID negocioId, UUID origenOfflineId);

    /** Las ventas "en espera" (borrador) de un cajero en una bodega, más reciente primero. */
    List<Venta> findByNegocioIdAndUsuarioIdAndBodegaIdAndEstadoOrderByCreadoEnDesc(UUID negocioId,
            UUID usuarioId, UUID bodegaId, EstadoVenta estado);
}
