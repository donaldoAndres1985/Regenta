package com.regenta.comandas.domain;

import com.regenta.comun.errores.ReglaDeNegocioException;

/** Cómo se cobró una cuenta (HU-090). A diferencia del curso o el modo de división, no
 * tiene un valor por defecto razonable: si no se dice cómo pagó, se rechaza. */
public enum MetodoDePago {
    EFECTIVO, TARJETA_DEBITO, TARJETA_CREDITO, TRANSFERENCIA, QR, BONO, CARGO_HABITACION, OTRO;

    public static MetodoDePago desde(String texto) {
        if (texto == null || texto.isBlank()) {
            throw new ReglaDeNegocioException("El método de pago es obligatorio");
        }
        try {
            return valueOf(texto.trim().toUpperCase());
        } catch (IllegalArgumentException noExiste) {
            throw new ReglaDeNegocioException("Ese método de pago no existe: " + texto);
        }
    }
}
