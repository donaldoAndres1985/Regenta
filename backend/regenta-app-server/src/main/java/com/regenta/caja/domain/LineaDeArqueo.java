package com.regenta.caja.domain;

import java.math.BigDecimal;

/**
 * Una línea del arqueo de cierre: esperado, contado y diferencia de un método de pago.
 * La diferencia es contado − esperado (I4b): negativa = faltante, positiva = sobrante.
 */
public record LineaDeArqueo(MetodoPagoCaja metodo, boolean cuadrable, BigDecimal esperado,
        BigDecimal contado, long movimientos) {

    public BigDecimal diferencia() {
        return contado.subtract(esperado);
    }

    public boolean cuadra() {
        return diferencia().signum() == 0;
    }
}
