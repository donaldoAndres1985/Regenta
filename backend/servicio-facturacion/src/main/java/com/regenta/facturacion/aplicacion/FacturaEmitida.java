package com.regenta.facturacion.aplicacion;

import java.math.BigDecimal;
import java.util.UUID;

import com.regenta.facturacion.domain.Factura;

/** El resultado de emitir: id, número y total. */
public record FacturaEmitida(
        UUID id,
        String numeroCompleto,
        String origenTipo,
        String estado,
        BigDecimal total) {

    static FacturaEmitida de(Factura f) {
        return new FacturaEmitida(f.getId(), f.getNumeroCompleto(), f.getOrigenTipo().name(),
                f.getEstado().name(), f.getTotal());
    }
}
