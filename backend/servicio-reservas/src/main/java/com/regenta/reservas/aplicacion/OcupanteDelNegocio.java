package com.regenta.reservas.aplicacion;

import java.time.LocalDate;
import java.util.UUID;

import com.regenta.reservas.domain.Ocupante;

/** Un ocupante como lo ve la recepción (HU-072). */
public record OcupanteDelNegocio(
        UUID id,
        boolean esTitular,
        String nombres,
        String apellidos,
        String tipoDocumento,
        String numeroDocumento,
        String nacionalidad,
        LocalDate fechaNacimiento,
        String telefono,
        String email) {

    static OcupanteDelNegocio de(Ocupante o) {
        return new OcupanteDelNegocio(o.getId(), o.esTitular(), o.getNombres(), o.getApellidos(),
                o.getTipoDocumento(), o.getNumeroDocumento(), o.getNacionalidad(),
                o.getFechaNacimiento(), o.getTelefono(), o.getEmail());
    }
}
