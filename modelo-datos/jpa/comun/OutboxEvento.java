package com.regenta.comun.mensajeria;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Transactional Outbox.
 *
 * Por qué existe: guardar la venta y publicar en RabbitMQ son dos sistemas
 * distintos. Si se publica después del commit y el broker está caído, el
 * evento se pierde y Facturación nunca emite la factura. Con Outbox el
 * evento se INSERTA en la misma transacción que la venta (atómico), y un
 * publicador aparte lo envía y lo marca PUBLICADO.
 */
@Entity
@Table(name = "outbox_eventos")
public class OutboxEvento {

    @Id private UUID id;

    @Column(name = "negocio_id", nullable = false) private UUID negocioId;
    @Column(name = "agregado_tipo", nullable = false) private String agregadoTipo;
    @Column(name = "agregado_id", nullable = false)   private UUID agregadoId;
    @Column(name = "tipo_evento", nullable = false)   private String tipoEvento;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> payload;

    @Column(name = "trace_id") private String traceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    private Estado estado = Estado.PENDIENTE;

    @Column(name = "intentos", nullable = false) private int intentos = 0;
    @Column(name = "ultimo_error") private String ultimoError;
    @Column(name = "creado_en", nullable = false) private OffsetDateTime creadoEn = OffsetDateTime.now();
    @Column(name = "publicado_en") private OffsetDateTime publicadoEn;

    public enum Estado { PENDIENTE, PUBLICADO, FALLIDO }

    public static OutboxEvento de(UUID negocioId, String agregadoTipo, UUID agregadoId,
                                  String tipoEvento, Map<String, Object> payload) {
        OutboxEvento e = new OutboxEvento();
        e.id = UUID.randomUUID();
        e.negocioId = negocioId;
        e.agregadoTipo = agregadoTipo;
        e.agregadoId = agregadoId;
        e.tipoEvento = tipoEvento;
        e.payload = payload;
        return e;
    }

    public void marcarPublicado() {
        this.estado = Estado.PUBLICADO;
        this.publicadoEn = OffsetDateTime.now();
    }

    public void marcarFallido(String error) {
        this.intentos++;
        this.ultimoError = error;
        if (this.intentos >= 10) this.estado = Estado.FALLIDO;   // a DLQ
    }
}
