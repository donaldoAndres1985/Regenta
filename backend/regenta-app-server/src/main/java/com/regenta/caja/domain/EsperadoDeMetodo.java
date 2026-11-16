package com.regenta.caja.domain;

import java.math.BigDecimal;

/** Lo que el sistema espera de un método de pago en una sesión (I7): suma con signo. */
public record EsperadoDeMetodo(MetodoPagoCaja metodo, BigDecimal esperado, long movimientos) {
}
