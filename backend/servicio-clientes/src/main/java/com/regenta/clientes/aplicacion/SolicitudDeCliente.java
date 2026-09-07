package com.regenta.clientes.aplicacion;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Alta o edicion de un cliente. Lo minimo depende del tipo de persona y se
 * valida en el servicio, no aqui: una JURIDICA necesita {@code razonSocial}, una
 * NATURAL con documento necesita {@code nombres}, y {@code SIN_IDENTIFICAR} no
 * necesita nada —es el «consumidor final».
 */
public record SolicitudDeCliente(
        @Size(max = 20) String tipoPersona,
        @Size(max = 10) String tipoDocumento,
        @Size(max = 30) String numeroDocumento,
        @Size(max = 1) String digitoVerificacion,
        @Size(max = 120) String nombres,
        @Size(max = 120) String apellidos,
        @Size(max = 200) String razonSocial,
        @Email @Size(max = 150) String email,
        @Size(max = 30) String telefono,
        @Size(max = 30) String telefonoAlterno,
        @Size(max = 40) String segmento,
        @Pattern(regexp = "(?s).{0,4000}", message = "las notas no pueden pasar de 4000 caracteres")
        String notas) {
}
