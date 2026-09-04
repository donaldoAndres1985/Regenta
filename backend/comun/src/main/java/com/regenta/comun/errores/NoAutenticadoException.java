package com.regenta.comun.errores;

import org.springframework.http.HttpStatus;

/**
 * Credenciales que no sirven. El detalle nunca dice cual de las dos cosas
 * fallo: decir "ese correo no existe" es regalar la mitad del trabajo.
 */
public class NoAutenticadoException extends ErrorDeAplicacion {

    private static final long serialVersionUID = 1L;

    public NoAutenticadoException(String detalle) {
        super(HttpStatus.UNAUTHORIZED, "No autenticado", detalle);
    }
}
