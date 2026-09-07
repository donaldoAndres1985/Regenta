package com.regenta.inventario.aplicacion;

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
