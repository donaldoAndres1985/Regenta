package com.regenta.ventas.application;

import java.math.BigDecimal;
import java.util.UUID;

import com.regenta.ventas.domain.MotivoDevolucion;

public record DevolucionDelNegocio(
        UUID id,
        String numero,
        String tipo,
        MotivoDevolucion motivo,
        boolean reintegraStock,
        BigDecimal total) {
}
