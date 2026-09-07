package com.regenta.clientes.aplicacion;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** El estado de crédito y cartera de un cliente (HU-022). */
public record CarteraDelCliente(
        UUID clienteId,
        boolean creditoHabilitado,
        BigDecimal cupo,
        BigDecimal saldo,
        BigDecimal disponible,
        int diasCredito,
        List<CuentaEnCartera> cuentas) {
}
