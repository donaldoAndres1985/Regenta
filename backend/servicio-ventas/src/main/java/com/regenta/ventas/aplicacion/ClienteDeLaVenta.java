package com.regenta.ventas.aplicacion;

import java.util.List;
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
        String digitoVerificacion,
        /** HU-133: con qué régimen se le factura; {@code null} si el CRM no lo tiene. */
        String regimenFiscal,
        List<String> responsabilidadesFiscales) {

    public ClienteDeLaVenta {
        responsabilidadesFiscales = responsabilidadesFiscales == null
                ? List.of() : List.copyOf(responsabilidadesFiscales);
    }

    /** El cliente de antes de HU-133, sin datos fiscales. */
    public ClienteDeLaVenta(UUID id, String nombre, String tipoDocumento, String numeroDocumento,
            String digitoVerificacion) {
        this(id, nombre, tipoDocumento, numeroDocumento, digitoVerificacion, null, List.of());
    }
}
