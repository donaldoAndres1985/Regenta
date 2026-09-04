package com.regenta.comun.eventos;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Un evento que ya llego.
 *
 * <p>La clave primaria es el {@code message-id} de AMQP. RabbitMQ entrega
 * <em>at-least-once</em>: el mismo evento puede llegar dos veces —por un reintento,
 * por un consumidor que murio antes del ack— y sin esta tabla se facturaria dos
 * veces. El segundo insert choca contra la PK y ahi se corta el efecto.
 */
@Entity
@Table(name = "inbox_eventos")
public class InboxEvento {

    @Id
    @Column(name = "mensaje_id")
    private UUID mensajeId;

    @Column(name = "negocio_id", nullable = false)
    private UUID negocioId;

    @Column(name = "tipo_evento", nullable = false, length = 80)
    private String tipoEvento;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload")
    private String payload;

    @Column(name = "recibido_en", nullable = false)
    private OffsetDateTime recibidoEn = OffsetDateTime.now();

    @Column(name = "procesado_en")
    private OffsetDateTime procesadoEn;

    @Column(name = "estado", nullable = false, length = 20)
    private String estado = EstadoInbox.RECIBIDO.name();

    @Column(name = "intentos", nullable = false)
    private int intentos;

    @Column(name = "ultimo_error")
    private String ultimoError;

    protected InboxEvento() {
        // para JPA
    }

    public InboxEvento(UUID mensajeId, UUID negocioId, String tipoEvento, String payload) {
        this.mensajeId = mensajeId;
        this.negocioId = negocioId;
        this.tipoEvento = tipoEvento;
        this.payload = payload;
    }

    public void marcarProcesado() {
        this.estado = EstadoInbox.PROCESADO.name();
        this.procesadoEn = OffsetDateTime.now();
    }

    public void marcarError(String error) {
        this.intentos++;
        this.estado = EstadoInbox.ERROR.name();
        this.ultimoError = error != null && error.length() > 2000 ? error.substring(0, 2000) : error;
    }

    public UUID getMensajeId() {
        return mensajeId;
    }

    public UUID getNegocioId() {
        return negocioId;
    }

    public String getTipoEvento() {
        return tipoEvento;
    }

    public String getPayload() {
        return payload;
    }

    public EstadoInbox getEstado() {
        return EstadoInbox.valueOf(estado);
    }

    public OffsetDateTime getProcesadoEn() {
        return procesadoEn;
    }
}
