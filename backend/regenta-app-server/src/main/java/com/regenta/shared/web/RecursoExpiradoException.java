package com.regenta.shared.web;

import org.springframework.http.HttpStatus;

/** Existio y ya no sirve: una invitacion vencida, por ejemplo. 410. */
public class RecursoExpiradoException extends ErrorDeAplicacion {

    private static final long serialVersionUID = 1L;

    public RecursoExpiradoException(String detalle) {
        super(HttpStatus.GONE, "Expirado", detalle);
    }
}
