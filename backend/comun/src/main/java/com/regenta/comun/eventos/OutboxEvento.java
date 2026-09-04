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
 * Un evento esperando salir.
 *
 * <p>Se inserta en la MISMA transaccion que el agregado que lo produce. Esa es toda
 * la idea: si el commit falla, no queda ni el agregado ni el evento; si el commit
 * pasa, el evento esta guardado aunque RabbitMQ este caido, y sale despues.
 *
 * <p>Publicar dentro del metodo, antes del commit, es lo que no se puede hacer: el
 * broker recibiria un evento de algo que todavia puede no ocurrir.
 */
@Entity
@Table(name = "outbox_eventos")
public class OutboxEvento {

    @Id
    private UUID id;

    @Column(name = "negocio_id", nullable = false)
    private UUID negocioId;

    @Column(name = "agregado_tipo", nullable = false, length = 60)
    private String agregadoTipo;

    @Column(name = "agregado_id", nullable = false)
    private UUID agregadoId;

    @Column(name = "tipo_evento", nullable = false, length = 80)
    private String tipoEvento;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false)
    private String payload;

    @Column(name = "trace_id", length = 64)
    private String traceId;

    @Column(name = "estado", nullable = false, length = 20)
    private String estado = EstadoOutbox.PENDIENTE.name();

    @Column(name = "intentos", nullable = false)
    private int intentos;

    @Column(name = "ultimo_error")
    private String ultimoError;

    @Column(name = "creado_en", nullable = false)
    private OffsetDateTime creadoEn = OffsetDateTime.now();

    @Column(name = "publicado_en")
    private OffsetDateTime publicadoEn;

    protected OutboxEvento() {
        // para JPA
    }

    public OutboxEvento(UUID id, UUID negocioId, String agregadoTipo, UUID agregadoId,
                        String tipoEvento, String payload, String traceId) {
        this.id = id;
        this.negocioId = negocioId;
        this.agregadoTipo = agregadoTipo;
        this.agregadoId = agregadoId;
        this.tipoEvento = tipoEvento;
        this.payload = payload;
        this.traceId = traceId;
    }

    public void marcarPublicado() {
        this.estado = EstadoOutbox.PUBLICADO.name();
        this.publicadoEn = OffsetDateTime.now();
        this.ultimoError = null;
    }

    /** Suma un intento y, si se agotaron, lo da por fallido. Devuelve si ya se agoto. */
    public boolean marcarIntentoFallido(String error, int maximoIntentos) {
        this.intentos++;
        this.ultimoError = error != null && error.length() > 2000 ? error.substring(0, 2000) : error;
        if (this.intentos >= maximoIntentos) {
            this.estado = EstadoOutbox.FALLIDO.name();
            return true;
        }
        return false;
    }

    public UUID getId() {
        return id;
    }

    public UUID getNegocioId() {
        return negocioId;
    }

    public String getAgregadoTipo() {
        return agregadoTipo;
    }

    public UUID getAgregadoId() {
        return agregadoId;
    }

    public String getTipoEvento() {
        return tipoEvento;
    }

    public String getPayload() {
        return payload;
    }

    public String getTraceId() {
        return traceId;
    }

    public EstadoOutbox getEstado() {
        return EstadoOutbox.valueOf(estado);
    }

    public int getIntentos() {
        return intentos;
    }

    public String getUltimoError() {
        return ultimoError;
    }

    public OffsetDateTime getPublicadoEn() {
        return publicadoEn;
    }
}
