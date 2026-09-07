package com.regenta.inventario.infra;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.regenta.inventario.domain.MovimientoInventario;
import com.regenta.inventario.domain.MovimientoInventarioId;

public interface MovimientoInventarioRepositorio
        extends JpaRepository<MovimientoInventario, MovimientoInventarioId> {

    Optional<MovimientoInventario> findFirstByNegocioIdAndIdempotencyKey(
            UUID negocioId, String idempotencyKey);

    List<MovimientoInventario> findByProductoIdAndBodegaIdOrderByOcurridoEnAsc(
            UUID productoId, UUID bodegaId);

    List<MovimientoInventario> findByProductoIdOrderByOcurridoEnAsc(UUID productoId);
}
