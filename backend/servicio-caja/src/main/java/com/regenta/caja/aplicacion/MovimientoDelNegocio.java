package com.regenta.caja.aplicacion;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.regenta.caja.domain.MovimientoDeCaja;

/** Un movimiento de una sesión de caja (HU-060). */
public record MovimientoDelNegocio(
        UUID id,
        String tipo,
        int signo,
        String metodoPago,
        BigDecimal monto,
        BigDecimal propina,
        String origenTipo,
        UUID origenId,
        String documentoRef,
        String concepto,
        OffsetDateTime ocurridoEn) {

    static MovimientoDelNegocio de(MovimientoDeCaja m) {
        return new MovimientoDelNegocio(m.getId(), m.getTipo().name(), m.getSigno(),
                m.getMetodoPago().name(), m.getMonto(), m.getPropina(), m.getOrigenTipo(),
                m.getOrigenId(), m.getDocumentoRef(), m.getConcepto(), m.getOcurridoEn());
    }
}
