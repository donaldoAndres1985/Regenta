package com.regenta.menu.aplicacion;

import java.math.BigDecimal;
import java.util.UUID;

import com.regenta.menu.domain.Modificador;

/** Un modificador como lo ve el administrador (HU-078). */
public record ModificadorDelNegocio(
        UUID id,
        UUID grupoId,
        String nombre,
        BigDecimal precioExtra,
        UUID productoId,
        BigDecimal cantidadInsumo,
        int orden,
        boolean activo) {

    static ModificadorDelNegocio de(Modificador m) {
        return new ModificadorDelNegocio(m.getId(), m.getGrupoId(), m.getNombre(),
                m.getPrecioExtra(), m.getProductoId(), m.getCantidadInsumo(), m.getOrden(),
                m.isActivo());
    }
}
