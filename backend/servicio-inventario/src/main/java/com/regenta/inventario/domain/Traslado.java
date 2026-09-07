package com.regenta.inventario.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import com.regenta.comun.errores.ConflictoDeEstadoException;
import com.regenta.comun.errores.ReglaDeNegocioException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * Un movimiento de mercancia de una bodega a otra. HU-032.
 *
 * <p>El ciclo es BORRADOR → EN_TRANSITO → RECIBIDO. Mientras esta EN_TRANSITO la
 * mercancia vive en una bodega de transito del negocio: no desaparece del
 * inventario entre que sale de una sede y llega a la otra.
 */
@Entity
@Table(name = "traslados")
public class Traslado {

    @Id
    private UUID id;

    @Column(name = "negocio_id", nullable = false, updatable = false)
    private UUID negocioId;

    @Column(nullable = false, length = 30, updatable = false)
    private String numero;

    @Column(name = "bodega_origen_id", nullable = false, updatable = false)
    private UUID bodegaOrigenId;

    @Column(name = "bodega_destino_id", nullable = false, updatable = false)
    private UUID bodegaDestinoId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoTraslado estado;

    @Column(name = "fecha_envio")
    private OffsetDateTime fechaEnvio;

    @Column(name = "fecha_recepcion")
    private OffsetDateTime fechaRecepcion;

    @Column(name = "usuario_envio_id")
    private UUID usuarioEnvioId;

    @Column(name = "usuario_recepcion_id")
    private UUID usuarioRecepcionId;

    @Column(columnDefinition = "text")
    private String observaciones;

    @CreationTimestamp
    @Column(name = "creado_en", nullable = false, updatable = false)
    private OffsetDateTime creadoEn;

    @Version
    @Column(nullable = false)
    private long version;

    protected Traslado() {
    }

    public static Traslado crear(UUID negocioId, String numero, UUID bodegaOrigenId,
            UUID bodegaDestinoId, String observaciones) {
        if (bodegaOrigenId.equals(bodegaDestinoId)) {
            throw new ReglaDeNegocioException(
                    "La bodega de origen y la de destino no pueden ser la misma");
        }
        Traslado t = new Traslado();
        t.id = UUID.randomUUID();
        t.negocioId = negocioId;
        t.numero = numero;
        t.bodegaOrigenId = bodegaOrigenId;
        t.bodegaDestinoId = bodegaDestinoId;
        t.observaciones = observaciones;
        t.estado = EstadoTraslado.BORRADOR;
        return t;
    }

    /** BORRADOR → EN_TRANSITO. La mercancia sale de origen y entra a transito. */
    public void enviar(UUID usuarioId) {
        exigirEstado(EstadoTraslado.BORRADOR, "enviar");
        this.estado = EstadoTraslado.EN_TRANSITO;
        this.usuarioEnvioId = usuarioId;
        this.fechaEnvio = OffsetDateTime.now();
    }

    /** EN_TRANSITO → RECIBIDO. La mercancia sale de transito y entra a destino. */
    public void recibir(UUID usuarioId) {
        exigirEstado(EstadoTraslado.EN_TRANSITO, "recibir");
        this.estado = EstadoTraslado.RECIBIDO;
        this.usuarioRecepcionId = usuarioId;
        this.fechaRecepcion = OffsetDateTime.now();
    }

    private void exigirEstado(EstadoTraslado esperado, String accion) {
        if (this.estado != esperado) {
            throw new ConflictoDeEstadoException("El traslado " + numero + " esta en " + estado
                    + " y no se puede " + accion);
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getNegocioId() {
        return negocioId;
    }

    public String getNumero() {
        return numero;
    }

    public UUID getBodegaOrigenId() {
        return bodegaOrigenId;
    }

    public UUID getBodegaDestinoId() {
        return bodegaDestinoId;
    }

    public EstadoTraslado getEstado() {
        return estado;
    }

    public OffsetDateTime getFechaEnvio() {
        return fechaEnvio;
    }

    public OffsetDateTime getFechaRecepcion() {
        return fechaRecepcion;
    }
}
