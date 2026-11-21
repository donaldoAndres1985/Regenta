package com.regenta.compras.application;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.regenta.compras.domain.CuentaPorPagar;
import com.regenta.compras.domain.PagoProveedor;

/**
 * Una cuenta por pagar con su avance (HU-049, HU-169, HU-170). {@code vencida} y {@code diasDeMora}
 * se calculan (I25); {@code enEsperaDeFactura} marca la que llegó con remisión y aún no tiene número
 * de factura (I22).
 */
public record CuentaDelNegocio(
        UUID id,
        UUID proveedorId,
        String proveedorNombre,
        UUID recepcionId,
        String recepcionNumero,
        String numeroFactura,
        boolean enEsperaDeFactura,
        BigDecimal monto,
        BigDecimal saldo,
        LocalDate fechaEmision,
        LocalDate fechaVencimiento,
        String estado,
        boolean vencida,
        long diasDeMora,
        List<PagoRegistrado> pagos) {

    public record PagoRegistrado(
            UUID id,
            BigDecimal monto,
            String metodo,
            String referencia,
            LocalDate fecha,
            boolean desdeLaCaja) {

        /** HU-171 · {@code montoAplicado} es lo que ese pago abonó a esta cuenta, no el pago entero. */
        static PagoRegistrado de(PagoProveedor p, BigDecimal montoAplicado) {
            return new PagoRegistrado(p.getId(), montoAplicado, p.getMetodo().name(),
                    p.getReferencia(), p.getFecha(), p.salioDeLaCaja());
        }
    }

    static CuentaDelNegocio de(CuentaPorPagar c, LocalDate hoy, List<PagoRegistrado> pagos) {
        return de(c, hoy, pagos, null, null);
    }

    static CuentaDelNegocio de(CuentaPorPagar c, LocalDate hoy, List<PagoRegistrado> pagos,
            String proveedorNombre, String recepcionNumero) {
        return new CuentaDelNegocio(c.getId(), c.getProveedorId(), proveedorNombre,
                c.getRecepcionId(), recepcionNumero, c.getNumeroFactura(), c.enEsperaDeFactura(),
                c.getMonto(), c.getSaldo(), c.getFechaEmision(), c.getFechaVencimiento(),
                c.getEstado().name(), c.estaVencida(hoy), c.diasDeMora(hoy),
                pagos);
    }
}
