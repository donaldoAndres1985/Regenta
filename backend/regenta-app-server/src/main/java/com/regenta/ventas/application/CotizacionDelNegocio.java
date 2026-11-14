package com.regenta.ventas.application;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.regenta.ventas.domain.EstadoCotizacion;

public record CotizacionDelNegocio(
        UUID id,
        String numero,
        EstadoCotizacion estado,
        LocalDate validaHasta,
        BigDecimal total,
        UUID ventaId,
        UUID clienteId,
        List<LineaDeCotizacionDelNegocio> lineas) {

    public CotizacionDelNegocio(UUID id, String numero, EstadoCotizacion estado, LocalDate validaHasta,
            BigDecimal total, UUID ventaId, UUID clienteId) {
        this(id, numero, estado, validaHasta, total, ventaId, clienteId, List.of());
    }

    public record LineaDeCotizacionDelNegocio(
            short linea,
            UUID productoId,
            String sku,
            String nombre,
            String unidad,
            BigDecimal cantidad,
            BigDecimal precioUnitario,
            BigDecimal descuentoPct,
            BigDecimal impuestoPct,
            BigDecimal total) {
    }
}

