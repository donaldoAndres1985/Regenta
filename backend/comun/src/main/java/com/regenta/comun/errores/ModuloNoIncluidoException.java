package com.regenta.comun.errores;

import org.springframework.http.HttpStatus;

/**
 * El modulo no esta activo para este negocio. 402 y no 403 a proposito: el
 * usuario tiene el permiso, lo que falta es el modulo en el plan. HU-017.
 */
public class ModuloNoIncluidoException extends ErrorDeAplicacion {

    private static final long serialVersionUID = 1L;

    private final String modulo;

    public ModuloNoIncluidoException(String modulo) {
        super(HttpStatus.PAYMENT_REQUIRED, "Modulo no incluido en el plan",
                "El modulo " + modulo + " no esta activo para este negocio");
        this.modulo = modulo;
    }

    public String getModulo() {
        return modulo;
    }
}
