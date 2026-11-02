package com.regenta.facturacion.domain;

import java.math.BigDecimal;
import java.util.List;

/**
 * Una línea tal como llega en el evento de cierre. Los valores calculados
 * ({@code descuentoValor}, {@code baseGravable}, {@code total}) pueden venir
 * nulos: {@link Factura#emitir} los deriva.
 */
public record LineaFacturable(
        String codigo,
        String descripcion,
        BigDecimal cantidad,
        String unidadCodigo,
        BigDecimal precioUnitario,
        BigDecimal descuentoPct,
        BigDecimal descuentoValor,
        BigDecimal baseGravable,
        BigDecimal total,
        List<ImpuestoFacturable> impuestos) {

    public List<ImpuestoFacturable> impuestos() {
        return impuestos == null ? List.of() : impuestos;
    }

    /** Un impuesto de la línea; {@code base} y {@code valor} pueden venir nulos. */
    public record ImpuestoFacturable(
            String codigo,
            String nombre,
            BigDecimal porcentaje,
            BigDecimal base,
            BigDecimal valor,
            boolean esRetencion) {
    }
}
