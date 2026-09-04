package com.regenta.comun.errores;

import org.springframework.http.HttpStatus;

/** Demasiados intentos fallidos: la cuenta queda bloqueada un rato. 423. */
public class CuentaBloqueadaException extends ErrorDeAplicacion {

    private static final long serialVersionUID = 1L;

    public CuentaBloqueadaException(String detalle) {
        super(HttpStatus.LOCKED, "Cuenta bloqueada", detalle);
    }
}
