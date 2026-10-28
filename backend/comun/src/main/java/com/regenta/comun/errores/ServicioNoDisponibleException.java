package com.regenta.comun.errores;

import org.springframework.http.HttpStatus;

/**
 * Otro servicio del que depende la operación no respondió, o respondió con un
 * error suyo. 503. Es distinto de un 404: "no se sabe" nunca se disfraza de
 * "no existe" ni de una lista vacía que parezca válida (HU-138 criterio 10).
 */
public class ServicioNoDisponibleException extends ErrorDeAplicacion {

    private static final long serialVersionUID = 1L;

    public ServicioNoDisponibleException(String detalle) {
        super(HttpStatus.SERVICE_UNAVAILABLE, "Servicio no disponible", detalle);
    }
}
