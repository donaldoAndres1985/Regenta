package com.regenta.comun.negocio;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Exige un permiso del catalogo: MODULO_RECURSO_ACCION. HU-016.
 *
 * <p>Aqui la respuesta si es 403: el usuario esta autenticado y su negocio
 * tiene el modulo, pero su rol no alcanza. No confundir con
 * {@link RequiereModulo}, que responde 402 porque lo que falta es el plan.
 */
@Documented
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface RequierePermiso {

    String value();
}
