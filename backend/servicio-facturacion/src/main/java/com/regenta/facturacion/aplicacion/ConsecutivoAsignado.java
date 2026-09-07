package com.regenta.facturacion.aplicacion;

import java.util.UUID;

/**
 * Un número tomado del rango de una resolución (HU-054). {@code agotada} es true
 * cuando este fue el último del rango.
 */
public record ConsecutivoAsignado(
        UUID resolucionId,
        String tipoDocumento,
        String prefijo,
        long numero,
        String numeroCompleto,
        boolean agotada) {
}
