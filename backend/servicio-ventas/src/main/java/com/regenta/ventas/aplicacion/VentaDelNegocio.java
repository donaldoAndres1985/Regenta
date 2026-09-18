package com.regenta.ventas.aplicacion;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.regenta.ventas.domain.EstadoVenta;

public record VentaDelNegocio(
        UUID id,
        String numero,
        EstadoVenta estado,
        BigDecimal subtotal,
        BigDecimal descuentoTotal,
        BigDecimal baseGravable,
        BigDecimal impuestoTotal,
        BigDecimal total,
        BigDecimal costoTotal,
        // HU-113: quien compra. clienteId en NULL es consumidor final, que es
        // el camino corto y el que mas se usa en mostrador. El snapshot va al
        // lado porque es lo que va a salir en la factura, pase lo que pase
        // despues en el CRM.
        UUID clienteId,
        String clienteSnapshot,
        List<LineaDeVenta> lineas) {
}
