package com.regenta.ventas.aplicacion;

import java.util.UUID;

/**
 * Lo que Ventas necesita saber de un cliente (HU-113): a quién se le factura.
 * El resto de la ficha —teléfono, segmento, notas— es de servicio-clientes y
 * aquí no hace falta.
 */
public record ClienteDeLaVenta(
        UUID id,
        String nombre,
        String tipoDocumento,
        String numeroDocumento,
        String digitoVerificacion) {
}
