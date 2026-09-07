package com.regenta.inventario.aplicacion;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.regenta.inventario.domain.OrigenMovimiento;
import com.regenta.inventario.domain.TipoMovimiento;

public record RegistroDeMovimiento(
        UUID id,
        UUID productoId,
        UUID bodegaId,
        TipoMovimiento tipo,
        short signo,
        BigDecimal cantidad,
        BigDecimal saldoPosterior,
        OrigenMovimiento origenTipo,
        OffsetDateTime ocurridoEn,
        boolean duplicado) {
}
