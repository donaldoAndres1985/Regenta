package com.regenta.comun.negocio;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Exige que el negocio tenga el modulo activo. HU-017.
 *
 * <p>Ocultar el boton en la app es cortesia; esto es la defensa. Si el modulo
 * no esta, la respuesta es 402 y no 403: el usuario tiene el permiso, lo que
 * falta es el modulo en su plan.
 */
@Documented
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiereModulo {

    /** Codigo del modulo, como en la tabla {@code modulos}: FACTURACION, VENTAS... */
    String value();
}
