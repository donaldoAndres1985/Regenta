package com.regenta.ventas.aplicacion;

import java.util.UUID;

/**
 * El resultado de convertir una cotización. {@code preciosDesactualizados} es
 * true si la cotización ya había vencido: se convirtió igual, pero conviene
 * revisar los precios antes de confirmar (HU-044, criterio 2).
 */
public record ResultadoDeConversion(
        UUID ventaId,
        String numeroVenta,
        boolean preciosDesactualizados) {
}
