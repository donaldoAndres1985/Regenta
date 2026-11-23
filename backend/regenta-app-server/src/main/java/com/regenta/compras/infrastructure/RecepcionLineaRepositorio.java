package com.regenta.compras.infrastructure;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.regenta.compras.domain.RecepcionLinea;

public interface RecepcionLineaRepositorio extends JpaRepository<RecepcionLinea, UUID> {

    List<RecepcionLinea> findByRecepcionId(UUID recepcionId);

    /** HU-183 · Las líneas de varias recepciones, para los informes. */
    List<RecepcionLinea> findByRecepcionIdIn(java.util.Collection<UUID> recepcionIds);

    /** HU-183 C3 · Todas las veces que entró un producto. */
    List<RecepcionLinea> findByNegocioIdAndProductoId(UUID negocioId, UUID productoId);
}
