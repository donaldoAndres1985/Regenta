package com.regenta.inventario.application;

import com.regenta.inventario.api.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Lo que queda tras registrar una entrada por lote. */
public record RegistroDeLote(
        UUID loteId,
        String codigoLote,
        LocalDate fechaVencimiento,
        boolean vencido,
        BigDecimal cantidadEnBodega,
        RegistroDeMovimiento movimiento) {
}
