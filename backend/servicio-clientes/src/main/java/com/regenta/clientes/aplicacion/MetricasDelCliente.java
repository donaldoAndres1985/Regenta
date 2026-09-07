package com.regenta.clientes.aplicacion;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.regenta.clientes.domain.ClienteMetricas;

/** La vista de lectura de las metricas de un cliente. */
public record MetricasDelCliente(
        UUID clienteId,
        int totalDocumentos,
        BigDecimal montoTotal,
        BigDecimal ticketPromedio,
        OffsetDateTime primeraCompraEn,
        OffsetDateTime ultimaCompraEn) {

    static MetricasDelCliente de(ClienteMetricas m) {
        return new MetricasDelCliente(m.getClienteId(), m.getTotalDocumentos(), m.getMontoTotal(),
                m.getTicketPromedio(), m.getPrimeraCompraEn(), m.getUltimaCompraEn());
    }

    /** Un cliente sin compras todavia: la proyeccion aun no tiene fila suya. */
    static MetricasDelCliente vacias(UUID clienteId) {
        return new MetricasDelCliente(clienteId, 0, BigDecimal.ZERO, BigDecimal.ZERO, null, null);
    }
}
