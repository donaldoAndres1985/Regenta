package com.regenta.clientes.domain;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Proyeccion de lo que un cliente ha comprado (HU-023). NO se calcula con un
 * JOIN a Ventas: se arma consumiendo {@code venta_completada},
 * {@code estancia_finalizada} y {@code pedido_completado} —los tres patrones
 * alimentan la misma tabla— y se ajusta a la baja con {@code venta_anulada}.
 *
 * <p>La escritura va por SQL atomico en {@link com.regenta.clientes.infra
 * .ClienteMetricasRepositorio} ({@code INSERT ... ON CONFLICT}); esta entidad es
 * solo de lectura.
 */
@Entity
@Table(name = "cliente_metricas")
public class ClienteMetricas {

    @Id
    @Column(name = "cliente_id")
    private UUID clienteId;

    @Column(name = "negocio_id", nullable = false, updatable = false)
    private UUID negocioId;

    @Column(name = "total_documentos", nullable = false)
    private int totalDocumentos;

    @Column(name = "monto_total", nullable = false)
    private BigDecimal montoTotal;

    @Column(name = "ticket_promedio", nullable = false)
    private BigDecimal ticketPromedio;

    @Column(name = "primera_compra_en")
    private OffsetDateTime primeraCompraEn;

    @Column(name = "ultima_compra_en")
    private OffsetDateTime ultimaCompraEn;

    @Column(name = "actualizado_en", nullable = false)
    private OffsetDateTime actualizadoEn;

    protected ClienteMetricas() {
    }

    public UUID getClienteId() {
        return clienteId;
    }

    public UUID getNegocioId() {
        return negocioId;
    }

    public int getTotalDocumentos() {
        return totalDocumentos;
    }

    public BigDecimal getMontoTotal() {
        return montoTotal;
    }

    public BigDecimal getTicketPromedio() {
        return ticketPromedio;
    }

    public OffsetDateTime getPrimeraCompraEn() {
        return primeraCompraEn;
    }

    public OffsetDateTime getUltimaCompraEn() {
        return ultimaCompraEn;
    }

    public OffsetDateTime getActualizadoEn() {
        return actualizadoEn;
    }
}
