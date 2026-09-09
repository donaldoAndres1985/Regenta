package com.regenta.menu.aplicacion;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import com.regenta.menu.domain.ItemDeMenu;

/**
 * Un ítem tal como lo ve el mesero al abrir la carta (HU-077 criterio 4).
 * {@code disponible = false} lo muestra "agotado"; {@code pedible = false}
 * impide agregarlo a la comanda.
 */
public record ItemEnCarta(
        UUID id,
        String codigo,
        String nombre,
        String descripcion,
        String tipo,
        BigDecimal precio,
        Integer tiempoPreparacionMin,
        String curso,
        Map<String, Object> atributos,
        String imagenUrl,
        boolean disponible,
        boolean pedible) {

    static ItemEnCarta de(ItemDeMenu i) {
        return new ItemEnCarta(i.getId(), i.getCodigo(), i.getNombre(), i.getDescripcion(),
                i.getTipo().name(), i.getPrecio(),
                i.getTiempoPreparacionMin() == null ? null : (int) (short) i.getTiempoPreparacionMin(),
                i.getCurso() == null ? null : i.getCurso().name(), i.getAtributos(),
                i.getImagenUrl(), i.isDisponible(), i.pedible());
    }
}
