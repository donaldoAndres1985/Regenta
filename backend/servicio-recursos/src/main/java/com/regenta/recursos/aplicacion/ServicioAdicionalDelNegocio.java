package com.regenta.recursos.aplicacion;

import java.math.BigDecimal;
import java.util.UUID;

import com.regenta.recursos.domain.ServicioAdicional;

/** Un servicio adicional como lo ve el administrador (HU-068). */
public record ServicioAdicionalDelNegocio(
        UUID id,
        String codigo,
        String nombre,
        String descripcion,
        BigDecimal precio,
        UUID impuestoId,
        String modoCobro,
        UUID productoId,
        boolean descuentaInventario,
        boolean activo) {

    static ServicioAdicionalDelNegocio de(ServicioAdicional s) {
        return new ServicioAdicionalDelNegocio(s.getId(), s.getCodigo(), s.getNombre(),
                s.getDescripcion(), s.getPrecio(), s.getImpuestoId(), s.getModoCobro().name(),
                s.getProductoId(), s.descuentaInventario(), s.isActivo());
    }
}
