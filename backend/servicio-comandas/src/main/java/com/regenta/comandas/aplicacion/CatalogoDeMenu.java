package com.regenta.comandas.aplicacion;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Lo que {@code servicio-comandas} necesita de {@code servicio-menu} para armar
 * una línea: el ítem (nombre, precio, estación, curso, costo — HU-085 criterio 2)
 * y la validación de sus modificadores (HU-078, para el criterio 5).
 *
 * <p>Puerto: la implementación real es un cliente REST contra
 * {@code /api/menu/...}. El stub de tests replica solo lo que hace falta.
 */
public interface CatalogoDeMenu {

    /** El ítem del menú, o {@code null} si no existe / no está activo. */
    ItemDeMenu item(UUID negocioId, UUID itemMenuId);

    /**
     * Valida la selección de modificadores contra los mínimos y máximos de sus
     * grupos y devuelve las opciones con su precio. Lanza
     * {@link com.regenta.comun.errores.ReglaDeNegocioException} si falta un grupo
     * obligatorio o se pasa del máximo (HU-085 criterio 5).
     */
    Cotizacion cotizarModificadores(UUID negocioId, UUID itemMenuId, List<UUID> modificadorIds);

    record ItemDeMenu(
            UUID id,
            String nombre,
            BigDecimal precio,
            UUID estacionId,
            String curso,
            BigDecimal costoEstimado,
            boolean disponible) {
    }

    record Cotizacion(List<ModificadorElegido> elegidos, BigDecimal extraPorUnidad) {
    }

    record ModificadorElegido(UUID id, String nombre, BigDecimal precioExtra) {
    }
}
