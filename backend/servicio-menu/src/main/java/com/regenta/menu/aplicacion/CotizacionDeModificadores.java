package com.regenta.menu.aplicacion;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * El resultado de validar una selección de modificadores para un ítem (HU-078):
 * las opciones elegidas y cuánto suman al total de la línea (criterio 3). Si la
 * selección no respeta los mínimos y máximos de los grupos, el servicio lanza un
 * 422 en vez de devolver esto (criterios 1 y 2).
 */
public record CotizacionDeModificadores(List<ModificadorElegido> elegidos, BigDecimal extraTotal) {

    public record ModificadorElegido(UUID id, UUID grupoId, String grupoNombre, String nombre,
            BigDecimal precioExtra) {
    }
}
