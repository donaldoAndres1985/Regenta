package com.regenta.compras.aplicacion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.regenta.compras.domain.CuentaPorPagar;
import com.regenta.compras.domain.PagoProveedor;

/** Una cuenta por pagar con su avance y —marcado— si está vencida (HU-049). */
public record CuentaDelNegocio(
        UUID id,
        UUID proveedorId,
        UUID recepcionId,
        String numeroFactura,
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
            LocalDate fecha) {

        static PagoRegistrado de(PagoProveedor p) {
            return new PagoRegistrado(p.getId(), p.getMonto(), p.getMetodo().name(),
                    p.getReferencia(), p.getFecha());
        }
    }

    static CuentaDelNegocio de(CuentaPorPagar c, LocalDate hoy, List<PagoProveedor> pagos) {
        return new CuentaDelNegocio(c.getId(), c.getProveedorId(), c.getRecepcionId(),
                c.getNumeroFactura(), c.getMonto(), c.getSaldo(), c.getFechaEmision(),
                c.getFechaVencimiento(), c.getEstado().name(), c.estaVencida(hoy),
                c.diasDeMora(hoy), pagos.stream().map(PagoRegistrado::de).toList());
    }
}
