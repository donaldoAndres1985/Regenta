package com.regenta.clientes.aplicacion;

import java.math.BigDecimal;

/**
 * Si una venta a crédito cabe en el cupo del cliente (HU-022 criterio 1).
 * {@code cabe = false} viene con el motivo en {@code aviso}.
 */
public record ResultadoDeCupo(
        boolean cabe,
        BigDecimal cupo,
        BigDecimal saldo,
        BigDecimal disponible,
        String aviso) {
}
