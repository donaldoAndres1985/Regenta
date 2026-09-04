package com.regenta.comun.errores;

import org.springframework.http.HttpStatus;

/** La peticion esta bien formada pero rompe una regla del negocio. 422. */
public class ReglaDeNegocioException extends ErrorDeAplicacion {

    private static final long serialVersionUID = 1L;

    public ReglaDeNegocioException(String detalle) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, "Regla de negocio", detalle);
    }
}
