package com.regenta.clientes.aplicacion;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Lo que la proyeccion de metricas necesita de un evento de cierre, venga de
 * {@code venta_completada}, {@code estancia_finalizada}, {@code pedido_completado}
 * o {@code venta_anulada}. Se lee del payload con tolerancia: distintos emisores
 * nombran el monto {@code total} o {@code monto}, y el {@code cliente_id} puede
 * no venir (venta a consumidor final) —en ese caso no hay a quien sumarle.
 */
public record EventoDeCompra(UUID negocioId, UUID clienteId, BigDecimal monto,
        OffsetDateTime ocurridoEn) {

    public static EventoDeCompra desde(Map<String, Object> payload) {
        return new EventoDeCompra(
                uuid(payload.get("negocio_id")),
                uuid(payload.get("cliente_id")),
                monto(payload),
                fecha(payload.get("ocurrido_en")));
    }

    public boolean sinCliente() {
        return clienteId == null;
    }

    public BigDecimal montoOCero() {
        return monto == null ? BigDecimal.ZERO : monto;
    }

    public OffsetDateTime ocurridoOAhora() {
        return ocurridoEn == null ? OffsetDateTime.now() : ocurridoEn;
    }

    private static UUID uuid(Object valor) {
        return valor == null || valor.toString().isBlank() ? null : UUID.fromString(valor.toString());
    }

    private static BigDecimal monto(Map<String, Object> payload) {
        Object valor = payload.getOrDefault("total", payload.get("monto"));
        return valor == null ? null : new BigDecimal(valor.toString());
    }

    private static OffsetDateTime fecha(Object valor) {
        return valor == null || valor.toString().isBlank() ? null
                : OffsetDateTime.parse(valor.toString());
    }
}
