package com.regenta.comun.errores;

import org.springframework.http.HttpStatus;

/** No existe, o no existe para este negocio, que para el API es lo mismo. 404. */
public class NoEncontradoException extends ErrorDeAplicacion {

    private static final long serialVersionUID = 1L;

    public NoEncontradoException(String detalle) {
        super(HttpStatus.NOT_FOUND, "No encontrado", detalle);
    }
}
