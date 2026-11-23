package com.regenta.ventas.application;

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
        List<String> responsabilidadesFiscales,
        /** HU-132 · La lista de precios con que se le vende; {@code null}: la del negocio. */
        UUID listaPreciosId) {

    public ClienteDeLaVenta {
        responsabilidadesFiscales = responsabilidadesFiscales == null
                ? List.of() : List.copyOf(responsabilidadesFiscales);
    }

    /** El cliente de antes de HU-132, sin lista de precios propia. */
    public ClienteDeLaVenta(UUID id, String nombre, String tipoDocumento, String numeroDocumento,
            String digitoVerificacion, String regimenFiscal, List<String> responsabilidadesFiscales) {
        this(id, nombre, tipoDocumento, numeroDocumento, digitoVerificacion, regimenFiscal,
                responsabilidadesFiscales, null);
    }

    /** El cliente de antes de HU-133, sin datos fiscales. */
    public ClienteDeLaVenta(UUID id, String nombre, String tipoDocumento, String numeroDocumento,
            String digitoVerificacion) {
        this(id, nombre, tipoDocumento, numeroDocumento, digitoVerificacion, null, List.of(), null);
    }
}
