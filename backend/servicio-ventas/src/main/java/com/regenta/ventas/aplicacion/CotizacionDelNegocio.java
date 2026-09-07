package com.regenta.ventas.aplicacion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.regenta.ventas.domain.EstadoCotizacion;

public record CotizacionDelNegocio(
        UUID id,
        String numero,
        EstadoCotizacion estado,
        LocalDate validaHasta,
        BigDecimal total,
        UUID ventaId) {
}
