package com.regenta.comun.seguridad;

import java.lang.annotation.*;

/**
 * La validación de plan del CLAUDE.md, en el backend.
 * Ocultar el botón en Flutter no es control de acceso: si alguien manipula
 * la app o llama al API directo, esto es lo único que lo detiene.
 *
 *   @RequiereModulo("FACTURACION")
 *   @PostMapping("/facturas")
 *   ResponseEntity<FacturaDto> emitir(...)
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequiereModulo {
    String[] value();
    /** true = exige TODOS los módulos listados; false = basta con uno. */
    boolean todos() default true;
}
