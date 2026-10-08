package com.regenta.reservas.aplicacion;

import java.util.List;
import java.util.UUID;

/**
 * Lo que Reservas necesita saber del huésped al cerrar la estancia (HU-118):
 * a nombre de quién sale la factura. El resto de la ficha es de
 * servicio-clientes y aquí no hace falta.
 */
public record ClienteDeLaReserva(
        UUID id,
        String nombre,
        String tipoDocumento,
        String numeroDocumento,
        String digitoVerificacion,
        /** HU-133: con qué régimen se le factura; {@code null} si el CRM no lo tiene. */
        String regimenFiscal,
        List<String> responsabilidadesFiscales) {

    public ClienteDeLaReserva {
        responsabilidadesFiscales = responsabilidadesFiscales == null
                ? List.of() : List.copyOf(responsabilidadesFiscales);
    }

    /** El huésped de antes de HU-133, sin datos fiscales. */
    public ClienteDeLaReserva(UUID id, String nombre, String tipoDocumento,
            String numeroDocumento, String digitoVerificacion) {
        this(id, nombre, tipoDocumento, numeroDocumento, digitoVerificacion, null, List.of());
    }
}
