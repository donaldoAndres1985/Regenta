package com.regenta.comun.errores;

import org.springframework.http.HttpStatus;

/** Ya existe algo con esa identidad. 409. */
public class RecursoDuplicadoException extends ErrorDeAplicacion {

    private static final long serialVersionUID = 1L;

    public RecursoDuplicadoException(String detalle) {
        super(HttpStatus.CONFLICT, "Recurso duplicado", detalle);
    }
}
