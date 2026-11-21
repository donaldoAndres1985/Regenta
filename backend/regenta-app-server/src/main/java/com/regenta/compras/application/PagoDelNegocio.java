package com.regenta.compras.application;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.regenta.compras.domain.PagoProveedor;
import com.regenta.compras.domain.RepartoDePago;

/** HU-171 · Un pago a proveedor y lo que abonó a cada cuenta, con cómo quedó cada una. */
public record PagoDelNegocio(
        UUID id,
        UUID proveedorId,
        BigDecimal monto,
        String metodo,
        String referencia,
        LocalDate fecha,
        boolean desdeLaCaja,
        /** HU-172 · Si se anuló: cuándo, quién, por qué y qué sesión recibió el reverso. */
        boolean anulado,
        String motivoAnulacion,
        UUID anuladoPor,
        java.time.OffsetDateTime anuladoEn,
        UUID sesionReversoId,
        /** HU-171 C5 · Saldo a favor usado además del monto: lo aplicado suma monto + esto. */
        BigDecimal saldoAFavorUsado,
        List<Aplicada> aplicaciones) {

    public record Aplicada(UUID cuentaId, String numeroFactura, BigDecimal monto, BigDecimal saldo,
            String estado) {
    }

    static PagoDelNegocio de(PagoProveedor p, List<RepartoDePago.Abono> abonos) {
        return new PagoDelNegocio(p.getId(), p.getProveedorId(), p.getMonto(), p.getMetodo().name(),
                p.getReferencia(), p.getFecha(), p.salioDeLaCaja(), p.estaAnulado(),
                p.getMotivoAnulacion(), p.getAnuladoPor(), p.getAnuladoEn(), p.getSesionReversoId(),
                p.getSaldoAFavorUsado(),
                abonos.stream().map(a -> new Aplicada(a.cuenta().getId(), a.cuenta().getNumeroFactura(),
                        a.monto(), a.cuenta().getSaldo(), a.cuenta().getEstado().name())).toList());
    }
}
