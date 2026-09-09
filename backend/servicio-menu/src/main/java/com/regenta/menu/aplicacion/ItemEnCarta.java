package com.regenta.menu.aplicacion;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import com.regenta.menu.domain.ItemDeMenu;

/**
 * Un ítem tal como lo ve el mesero al abrir la carta (HU-077 criterio 4).
 * {@code disponible = false} lo muestra "agotado"; {@code pedible = false}
 * impide agregarlo a la comanda. El "se acabó" del día (HU-080) pesa igual que
 * el permanente: si está agotado hoy, no está ni disponible ni pedible.
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
        return de(i, false);
    }

    static ItemEnCarta de(ItemDeMenu i, boolean agotadoHoy) {
        return new ItemEnCarta(i.getId(), i.getCodigo(), i.getNombre(), i.getDescripcion(),
                i.getTipo().name(), i.getPrecio(),
                i.getTiempoPreparacionMin() == null ? null : (int) (short) i.getTiempoPreparacionMin(),
                i.getCurso() == null ? null : i.getCurso().name(), i.getAtributos(),
                i.getImagenUrl(), i.isDisponible() && !agotadoHoy, i.pedible() && !agotadoHoy);
    }
}
