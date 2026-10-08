package com.regenta.clientes.aplicacion;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Lo mínimo para facturar (HU-114): tipo y número de documento, nombre o razón
 * social, y correo. El resto de la ficha se completa después, en Clientes.
 *
 * @param id opcional: el que trae el dispositivo cuando el cliente se creó sin
 *           conexión (criterio 5). Reintentar la subida con el mismo id
 *           devuelve el mismo cliente en vez de chocar.
 */
public record SolicitudExpres(
        UUID id,
        @Size(max = 20) String tipoPersona,
        @NotBlank @Size(max = 10) String tipoDocumento,
        @Size(max = 30) String numeroDocumento,
        @Size(max = 1) String digitoVerificacion,
        @Size(max = 120) String nombres,
        @Size(max = 120) String apellidos,
        @Size(max = 200) String razonSocial,
        @Email @Size(max = 150) String email,
        /** Opcional: la pantalla lo ofrece, pero una factura no lo exige. */
        @Size(max = 30) String telefono,
        /** HU-133 criterio 2: opcional; sin él, la factura aplica el de persona natural. */
        @Size(max = 40) String regimenFiscal,
        List<@Size(max = 10) String> responsabilidadesFiscales) {

    /** El alta exprés de antes de HU-133, sin datos fiscales. */
    public SolicitudExpres(UUID id, String tipoPersona, String tipoDocumento,
            String numeroDocumento, String digitoVerificacion, String nombres, String apellidos,
            String razonSocial, String email, String telefono) {
        this(id, tipoPersona, tipoDocumento, numeroDocumento, digitoVerificacion, nombres,
                apellidos, razonSocial, email, telefono, null, null);
    }
}
