package com.regenta.compras.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import com.regenta.comun.errores.ConflictoDeEstadoException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * Una orden de compra: lo que se le va a pedir a un proveedor, con control de
 * aprobación antes de comprometerlo (HU-047).
 *
 * <p>En {@link EstadoOrdenCompra#BORRADOR} se arman y se editan las líneas.
 * {@link #aprobar(UUID, boolean)} deja registrado quién y cuándo (criterio 1) y
 * congela el contenido: intentar editar las líneas de una orden ya aprobada es
 * un conflicto de estado — 409 (criterio 3).
 */
@Entity
@Table(name = "ordenes_compra")
public class OrdenDeCompra {

    @Id
    private UUID id;

    @Column(name = "negocio_id", nullable = false, updatable = false)
    private UUID negocioId;

    @Column(name = "sucursal_id")
    private UUID sucursalId;

    @Column(nullable = false, length = 30, updatable = false)
    private String numero;

    @Column(name = "proveedor_id", nullable = false)
    private UUID proveedorId;

    @Column(name = "bodega_destino_id", nullable = false)
    private UUID bodegaDestinoId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoOrdenCompra estado;

    @Column(name = "fecha_emision", nullable = false)
    private LocalDate fechaEmision;

    @Column(name = "fecha_esperada")
    private LocalDate fechaEsperada;

    @Column(nullable = false)
    private BigDecimal subtotal;

    @Column(name = "descuento_total", nullable = false)
    private BigDecimal descuentoTotal;

    @Column(name = "impuesto_total", nullable = false)
    private BigDecimal impuestoTotal;

    @Column(nullable = false)
    private BigDecimal flete;

    @Column(nullable = false)
    private BigDecimal total;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(nullable = false, length = 3)
    private String moneda;

    @Column(name = "tasa_cambio", nullable = false)
    private BigDecimal tasaCambio;

    @Column(name = "usuario_id")
    private UUID usuarioId;

    @Column(name = "aprobado_por")
    private UUID aprobadoPor;

    @Column(name = "aprobado_en")
    private OffsetDateTime aprobadoEn;

    @Column(columnDefinition = "text")
    private String observaciones;

    @CreationTimestamp
    @Column(name = "creado_en", nullable = false, updatable = false)
    private OffsetDateTime creadoEn;

    @UpdateTimestamp
    @Column(name = "actualizado_en", nullable = false)
    private OffsetDateTime actualizadoEn;

    @Version
    @Column(nullable = false)
    private long version;

    protected OrdenDeCompra() {
    }

    public static OrdenDeCompra crear(UUID negocioId, String numero, UUID proveedorId,
            UUID bodegaDestinoId, UUID sucursalId, UUID usuarioId, LocalDate fechaEsperada,
            BigDecimal flete, String observaciones) {
        OrdenDeCompra o = new OrdenDeCompra();
        o.id = UUID.randomUUID();
        o.negocioId = negocioId;
        o.numero = numero;
        o.proveedorId = proveedorId;
        o.bodegaDestinoId = bodegaDestinoId;
        o.sucursalId = sucursalId;
        o.usuarioId = usuarioId;
        o.estado = EstadoOrdenCompra.BORRADOR;
        o.fechaEmision = LocalDate.now();
        o.fechaEsperada = fechaEsperada;
        o.flete = flete == null || flete.signum() < 0 ? BigDecimal.ZERO : flete;
        o.observaciones = observaciones;
        o.moneda = "COP";
        o.tasaCambio = BigDecimal.ONE;
        o.subtotal = BigDecimal.ZERO;
        o.descuentoTotal = BigDecimal.ZERO;
        o.impuestoTotal = BigDecimal.ZERO;
        o.total = BigDecimal.ZERO;
        return o;
    }

    /** Solo se editan las líneas en borrador (criterio 3). */
    public void exigirBorradorParaEditar() {
        if (estado != EstadoOrdenCompra.BORRADOR) {
            throw new ConflictoDeEstadoException(
                    "No se editan las líneas de una orden en estado " + estado);
        }
    }

    /** Vuelve a totalizar la cabecera a partir de las líneas ya calculadas. */
    public void totalizar(List<OrdenCompraLinea> lineas) {
        BigDecimal bruto = BigDecimal.ZERO;
        BigDecimal descuento = BigDecimal.ZERO;
        BigDecimal impuesto = BigDecimal.ZERO;
        for (OrdenCompraLinea l : lineas) {
            bruto = bruto.add(l.brutoDeLinea());
            descuento = descuento.add(l.descuentoDeLinea());
            impuesto = impuesto.add(l.impuestoDeLinea());
        }
        this.subtotal = bruto.setScale(4, RoundingMode.HALF_UP);
        this.descuentoTotal = descuento.setScale(4, RoundingMode.HALF_UP);
        this.impuestoTotal = impuesto.setScale(4, RoundingMode.HALF_UP);
        this.total = bruto.subtract(descuento).add(impuesto).add(flete)
                .setScale(4, RoundingMode.HALF_UP);
    }

    /** Criterio 1: deja registrado quién aprobó y cuándo. */
    public void aprobar(UUID usuarioId, boolean tieneLineas) {
        if (estado != EstadoOrdenCompra.BORRADOR) {
            throw new ConflictoDeEstadoException(
                    "Solo se aprueba una orden en borrador; esta está " + estado);
        }
        if (!tieneLineas) {
            throw new ConflictoDeEstadoException("No se aprueba una orden sin líneas");
        }
        this.estado = EstadoOrdenCompra.APROBADA;
        this.aprobadoPor = usuarioId;
        this.aprobadoEn = OffsetDateTime.now();
    }

    /** Enviar al proveedor: solo después de aprobada. */
    public void enviar() {
        if (estado != EstadoOrdenCompra.APROBADA) {
            throw new ConflictoDeEstadoException(
                    "Se envía una orden aprobada; esta está " + estado);
        }
        this.estado = EstadoOrdenCompra.ENVIADA;
    }

    public UUID getId() {
        return id;
    }

    public UUID getNegocioId() {
        return negocioId;
    }

    public UUID getSucursalId() {
        return sucursalId;
    }

    public String getNumero() {
        return numero;
    }

    public UUID getProveedorId() {
        return proveedorId;
    }

    public UUID getBodegaDestinoId() {
        return bodegaDestinoId;
    }

    public EstadoOrdenCompra getEstado() {
        return estado;
    }

    public LocalDate getFechaEmision() {
        return fechaEmision;
    }

    public LocalDate getFechaEsperada() {
        return fechaEsperada;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public BigDecimal getDescuentoTotal() {
        return descuentoTotal;
    }

    public BigDecimal getImpuestoTotal() {
        return impuestoTotal;
    }

    public BigDecimal getFlete() {
        return flete;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public OffsetDateTime getCreadoEn() {
        return creadoEn;
    }

    public UUID getAprobadoPor() {
        return aprobadoPor;
    }

    public OffsetDateTime getAprobadoEn() {
        return aprobadoEn;
    }

    public String getObservaciones() {
        return observaciones;
    }

    /** Lo que la aplicación pasa por cada renglón al armar o editar la orden. */
    public record LineaPedida(UUID productoId, String nombre, BigDecimal cantidad,
            BigDecimal costoUnitario, BigDecimal descuentoPct, BigDecimal impuestoPct) {
    }
}
