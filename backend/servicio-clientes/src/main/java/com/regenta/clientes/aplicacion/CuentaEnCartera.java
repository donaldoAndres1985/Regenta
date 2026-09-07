package com.regenta.clientes.aplicacion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.regenta.clientes.domain.CuentaPorCobrar;

/** Una cuenta por cobrar como se ve en la cartera del cliente. */
public record CuentaEnCartera(
        UUID id,
        String origenTipo,
        String documentoRef,
        BigDecimal monto,
        BigDecimal saldo,
        String estado,
        LocalDate fechaVencimiento,
        boolean vencida,
        long diasMora) {

    static CuentaEnCartera de(CuentaPorCobrar c, LocalDate hoy) {
        return new CuentaEnCartera(c.getId(), c.getOrigenTipo().name(), c.getDocumentoRef(),
                c.getMonto(), c.getSaldo(), c.getEstado().name(), c.getFechaVencimiento(),
                c.estaVencida(hoy), c.diasDeMora(hoy));
    }
}
