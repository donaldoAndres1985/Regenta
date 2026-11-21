package com.regenta.compras.domain;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import com.regenta.shared.web.ReglaDeNegocioException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * HU-178 · Mercancía de una recepción confirmada que vuelve al proveedor (I27). No se edita: lo que
 * sale de la bodega y la nota crédito quedan registrados en la misma transacción.
 */
@Entity
@Table(name = "devoluciones_proveedor", schema = "compras")
public class DevolucionAProveedor {

    @Id
    private UUID id;

    @Column(name = "negocio_id", nullable = false, updatable = false)
    private UUID negocioId;

    @Column(nullable = false, length = 30, updatable = false)
    private String numero;

    @Column(name = "recepcion_id", nullable = false, updatable = false)
    private UUID recepcionId;

    @Column(name = "proveedor_id", nullable = false, updatable = false)
    private UUID proveedorId;

    @Column(name = "bodega_id", nullable = false, updatable = false)
    private UUID bodegaId;

    @Column(nullable = false, length = 200, updatable = false)
    private String motivo;

    @Column(name = "valor_nota", nullable = false, updatable = false)
    private BigDecimal valorNota;

    @Column(name = "usuario_id", updatable = false)
    private UUID usuarioId;

    @Column(nullable = false, updatable = false)
    private OffsetDateTime fecha;

    @CreationTimestamp
    @Column(name = "creado_en", nullable = false, updatable = false)
    private OffsetDateTime creadoEn;

    @Version
    @Column(nullable = false)
    private long version;

    protected DevolucionAProveedor() {
    }

    public static DevolucionAProveedor registrar(UUID negocioId, String numero, Recepcion recepcion, String motivo,
            BigDecimal valorNota, UUID usuarioId) {
        String limpio = motivo == null ? "" : motivo.trim();
        if (limpio.isEmpty()) {
            throw new ReglaDeNegocioException("Una devolución al proveedor exige un motivo");
        }
        if (valorNota == null || valorNota.signum() <= 0) {
            throw new ReglaDeNegocioException("La devolución no tiene valor: revisa las cantidades");
        }
        DevolucionAProveedor d = new DevolucionAProveedor();
        d.id = UUID.randomUUID();
        d.negocioId = negocioId;
        d.numero = numero;
        d.recepcionId = recepcion.getId();
        d.proveedorId = recepcion.getProveedorId();
        d.bodegaId = recepcion.getBodegaId();
        d.motivo = limpio.length() > 200 ? limpio.substring(0, 200) : limpio;
        d.valorNota = valorNota;
        d.usuarioId = usuarioId;
        d.fecha = OffsetDateTime.now();
        return d;
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

    public UUID getRecepcionId() {
        return recepcionId;
    }

    public UUID getProveedorId() {
        return proveedorId;
    }

    public UUID getBodegaId() {
        return bodegaId;
    }

    public String getMotivo() {
        return motivo;
    }

    public BigDecimal getValorNota() {
        return valorNota;
    }

    public UUID getUsuarioId() {
        return usuarioId;
    }

    public OffsetDateTime getFecha() {
        return fecha;
    }
}
