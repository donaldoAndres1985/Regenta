package com.regenta.compras.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import com.regenta.comun.errores.ConflictoDeEstadoException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * Lo que llegó de un proveedor contra una orden de compra (HU-048).
 *
 * <p>Se arma en {@link EstadoRecepcion#BORRADOR} con las líneas recibidas y sus
 * lotes; al {@link #confirmar()} se sube el inventario y —si trae la factura del
 * proveedor— se abre la cuenta por pagar con el vencimiento del plazo del
 * proveedor (criterio 6).
 */
@Entity
@Table(name = "recepciones")
public class Recepcion {

    @Id
    private UUID id;

    @Column(name = "negocio_id", nullable = false, updatable = false)
    private UUID negocioId;

    @Column(name = "orden_id", updatable = false)
    private UUID ordenId;

    @Column(name = "proveedor_id", nullable = false, updatable = false)
    private UUID proveedorId;

    @Column(name = "bodega_id", nullable = false, updatable = false)
    private UUID bodegaId;

    @Column(nullable = false, length = 30, updatable = false)
    private String numero;

    @Column(name = "factura_proveedor", length = 40)
    private String facturaProveedor;

    @Column(nullable = false)
    private OffsetDateTime fecha;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoRecepcion estado;

    @Column(nullable = false)
    private BigDecimal total;

    @Column(name = "usuario_id")
    private UUID usuarioId;

    @Column(columnDefinition = "text")
    private String observaciones;

    @CreationTimestamp
    @Column(name = "creado_en", nullable = false, updatable = false)
    private OffsetDateTime creadoEn;

    @Version
    @Column(nullable = false)
    private long version;

    protected Recepcion() {
    }

    public static Recepcion crear(UUID negocioId, String numero, UUID ordenId, UUID proveedorId,
            UUID bodegaId, String facturaProveedor, UUID usuarioId, String observaciones) {
        Recepcion r = new Recepcion();
        r.id = UUID.randomUUID();
        r.negocioId = negocioId;
        r.numero = numero;
        r.ordenId = ordenId;
        r.proveedorId = proveedorId;
        r.bodegaId = bodegaId;
        r.facturaProveedor = limpiar(facturaProveedor);
        r.usuarioId = usuarioId;
        r.observaciones = limpiar(observaciones);
        r.fecha = OffsetDateTime.now();
        r.estado = EstadoRecepcion.BORRADOR;
        r.total = BigDecimal.ZERO;
        return r;
    }

    public void totalizar(List<RecepcionLinea> lineas) {
        BigDecimal suma = BigDecimal.ZERO;
        for (RecepcionLinea l : lineas) {
            suma = suma.add(l.subtotal());
        }
        this.total = suma.setScale(4, RoundingMode.HALF_UP);
    }

    /** BORRADOR → CONFIRMADA. Confirmar dos veces es un conflicto de estado. */
    public void confirmar() {
        if (estado != EstadoRecepcion.BORRADOR) {
            throw new ConflictoDeEstadoException(
                    "Solo se confirma una recepción en borrador; esta está " + estado);
        }
        this.estado = EstadoRecepcion.CONFIRMADA;
    }

    /** Adjunta o corrige la factura del proveedor mientras la recepción no esté confirmada. */
    public void registrarFactura(String facturaProveedor) {
        String limpia = limpiar(facturaProveedor);
        if (limpia != null) {
            this.facturaProveedor = limpia;
        }
    }

    public boolean tieneFactura() {
        return facturaProveedor != null && !facturaProveedor.isBlank();
    }

    private static String limpiar(String v) {
        if (v == null) {
            return null;
        }
        String t = v.trim();
        return t.isEmpty() ? null : t;
    }

    public UUID getId() {
        return id;
    }

    public UUID getNegocioId() {
        return negocioId;
    }

    public UUID getOrdenId() {
        return ordenId;
    }

    public UUID getProveedorId() {
        return proveedorId;
    }

    public UUID getBodegaId() {
        return bodegaId;
    }

    public String getNumero() {
        return numero;
    }

    public String getFacturaProveedor() {
        return facturaProveedor;
    }

    public OffsetDateTime getFecha() {
        return fecha;
    }

    public EstadoRecepcion getEstado() {
        return estado;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public String getObservaciones() {
        return observaciones;
    }
}
