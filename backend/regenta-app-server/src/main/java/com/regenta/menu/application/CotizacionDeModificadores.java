package com.regenta.menu.application;

import com.regenta.menu.api.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * El resultado de validar una selecciÃ³n de modificadores para un Ã­tem (HU-078):
 * las opciones elegidas y cuÃ¡nto suman al total de la lÃ­nea (criterio 3). Si la
 * selecciÃ³n no respeta los mÃ­nimos y mÃ¡ximos de los grupos, el servicio lanza un
 * 422 en vez de devolver esto (criterios 1 y 2).
 */
public record CotizacionDeModificadores(List<ModificadorElegido> elegidos, BigDecimal extraTotal) {

    public record ModificadorElegido(UUID id, UUID grupoId, String grupoNombre, String nombre,
            BigDecimal precioExtra) {
    }
}
