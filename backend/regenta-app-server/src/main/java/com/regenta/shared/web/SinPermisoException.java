package com.regenta.shared.web;

import org.springframework.http.HttpStatus;

/** Autenticado, pero sin el permiso que exige la operacion. 403. */
public class SinPermisoException extends ErrorDeAplicacion {

    private static final long serialVersionUID = 1L;

    public SinPermisoException(String permiso) {
        super(HttpStatus.FORBIDDEN, "Sin permiso",
                "La operacion exige el permiso " + permiso);
    }
}
