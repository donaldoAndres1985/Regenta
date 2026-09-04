package com.regenta.comun.errores;

import org.springframework.http.HttpStatus;

/**
 * La operacion no cabe con el estado actual: borrar un rol de sistema, o uno
 * que todavia tiene gente asignada. 409.
 */
public class ConflictoDeEstadoException extends ErrorDeAplicacion {

    private static final long serialVersionUID = 1L;

    public ConflictoDeEstadoException(String detalle) {
        super(HttpStatus.CONFLICT, "Conflicto con el estado actual", detalle);
    }
}
