package com.regenta.menu.aplicacion;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import com.regenta.menu.domain.ItemDeMenu;

/** Un ítem de menú como lo ve el administrador (HU-077). */
public record ItemDelNegocio(
        UUID id,
        UUID categoriaMenuId,
        UUID estacionId,
        String codigo,
        String nombre,
        String descripcion,
        String tipo,
        BigDecimal precio,
        boolean precioIncluyeImpuesto,
        BigDecimal costoEstimado,
        Integer tiempoPreparacionMin,
        String curso,
        boolean disponible,
        Map<String, Object> atributos,
        String imagenUrl,
        int orden,
        boolean activo) {

    static ItemDelNegocio de(ItemDeMenu i) {
        return new ItemDelNegocio(i.getId(), i.getCategoriaMenuId(), i.getEstacionId(),
                i.getCodigo(), i.getNombre(), i.getDescripcion(), i.getTipo().name(), i.getPrecio(),
                i.isPrecioIncluyeImpuesto(), i.getCostoEstimado(),
                i.getTiempoPreparacionMin() == null ? null : (int) (short) i.getTiempoPreparacionMin(),
                i.getCurso() == null ? null : i.getCurso().name(), i.isDisponible(),
                i.getAtributos(), i.getImagenUrl(), i.getOrden(), i.isActivo());
    }
}
