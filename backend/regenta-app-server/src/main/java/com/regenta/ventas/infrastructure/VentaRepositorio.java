package com.regenta.ventas.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.regenta.ventas.domain.EstadoVenta;
import com.regenta.ventas.domain.Venta;

public interface VentaRepositorio extends JpaRepository<Venta, UUID>, VentaRepositorioCustom {

    /** HU-043 criterio 3: la clave con la que una subida repetida se reconoce. */
    Optional<Venta> findByNegocioIdAndOrigenOfflineId(UUID negocioId, UUID origenOfflineId);

    /** Las ventas "en espera" (borrador) de un cajero en una bodega, más reciente primero. */
    List<Venta> findByNegocioIdAndUsuarioIdAndBodegaIdAndEstadoOrderByCreadoEnDesc(UUID negocioId,
            UUID usuarioId, UUID bodegaId, EstadoVenta estado);

    /** Buscar ventas por consecutivo o cliente del negocio, más reciente primero. */
    @Query("SELECT v FROM Venta v WHERE v.negocioId = :negocioId AND (:q IS NULL OR :q = '' OR LOWER(v.numero) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(CAST(v.clienteSnapshot AS string)) LIKE LOWER(CONCAT('%', :q, '%'))) ORDER BY v.fecha DESC")
    List<Venta> buscar(@Param("negocioId") UUID negocioId, @Param("q") String q);
}
