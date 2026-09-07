package com.regenta.inventario.infra;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.regenta.inventario.domain.PrecioDeProducto;
import com.regenta.inventario.domain.PrecioDeProductoId;

public interface PrecioDeProductoRepositorio
        extends JpaRepository<PrecioDeProducto, PrecioDeProductoId> {

    List<PrecioDeProducto> findByListaIdAndProductoIdOrderByCantidadMinimaAsc(
            UUID listaId, UUID productoId);
}
