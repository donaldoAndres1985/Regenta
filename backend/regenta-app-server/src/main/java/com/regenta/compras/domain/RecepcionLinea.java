package com.regenta.compras.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.UUID;

import com.regenta.shared.web.ReglaDeNegocioException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Un renglón de lo que llegó: qué producto, cuánto, a qué costo, con qué descuento e IVA y —donde
 * el producto lo exige— con qué lote y vencimiento (HU-048, HU-165, HU-167).
 *
 * <p>El lote se captura aquí, no en la ficha del producto: el mismo medicamento entra con lotes
 * distintos cada semana. La tarifa de IVA y la regla de lote vienen del catálogo de Inventario,
 * nunca del cliente (I8, I16 de {@code docs/dominio/compras.md}).
 *
 * <p>Montos de la línea: {@link #costoNeto()} = costo × (1 − descuento); {@link #subtotal()} =
 * cantidad × costo neto (la base, sin IVA); {@link #impuesto()} = base × tarifa.
 */
@Entity
@Table(name = "recepcion_lineas", schema = "compras")
public class RecepcionLinea {

    private static final BigDecimal CIEN = new BigDecimal("100");

    @Id
    private UUID id;

    @Column(name = "negocio_id", nullable = false, updatable = false)
    private UUID negocioId;

    @Column(name = "recepcion_id", nullable = false, updatable = false)
    private UUID recepcionId;

    @Column(name = "orden_linea_id")
    private UUID ordenLineaId;

    @Column(name = "producto_id", nullable = false)
    private UUID productoId;

    @Column(name = "nombre_snapshot", length = 180)
    private String nombre;

    @Column(name = "codigo_proveedor", length = 60)
    private String codigoProveedor;

    @Column(nullable = false)
    private BigDecimal cantidad;

    @Column(name = "costo_unitario", nullable = false)
    private BigDecimal costoUnitario;

    @Column(name = "descuento_pct", nullable = false)
    private BigDecimal descuentoPct;

    @Column(name = "tarifa_iva", length = 10)
    private String tarifaIva;

    @Column(name = "impuesto_pct", nullable = false)
    private BigDecimal impuestoPct;

    /** HU-168 · Con qué costo unitario entra al inventario (I14). Lo fija la recepción al totalizar. */
    @Column(name = "costo_entrada")
    private BigDecimal costoEntrada;

    /** HU-168 · El IVA de la línea ya descontada su parte del descuento global. */
    @Column(name = "impuesto_valor")
    private BigDecimal impuestoValor;

    @Column(name = "codigo_lote", length = 60)
    private String codigoLote;

    @Column(name = "fecha_vencimiento")
    private LocalDate fechaVencimiento;

    @Column(name = "registro_sanitario", length = 60)
    private String registroSanitario;

    /** HU-179 · El precio de venta (IVA incluido) que se fijó al confirmar; nulo si no cambió. */
    @Column(name = "precio_venta_nuevo")
    private BigDecimal precioVentaNuevo;

    protected RecepcionLinea() {
    }

    /**
     * Lo que llegó en una línea. {@code tarifaIva}, {@code impuestoPct}, {@code manejaLotes} y
     * {@code perecedero} salen del catálogo de Inventario.
     */
    public record Datos(
            UUID ordenLineaId,
            UUID productoId,
            String nombre,
            String codigoProveedor,
            BigDecimal cantidad,
            BigDecimal costoUnitario,
            BigDecimal descuentoPct,
            String tarifaIva,
            BigDecimal impuestoPct,
            boolean manejaLotes,
            boolean perecedero,
            String codigoLote,
            LocalDate fechaVencimiento,
            String registroSanitario) {
    }

    /**
     * Si el producto maneja lotes, el código es obligatorio; si es perecedero, también el
     * vencimiento; y un vencimiento anterior a la fecha de recepción se rechaza (I16).
     */
    public static RecepcionLinea nueva(UUID recepcionId, UUID negocioId, Datos d,
            LocalDate fechaRecepcion) {
        if (d.cantidad() == null || d.cantidad().signum() <= 0) {
            throw new ReglaDeNegocioException("La cantidad recibida debe ser mayor que cero");
        }
        if (d.costoUnitario() == null || d.costoUnitario().signum() < 0) {
            throw new ReglaDeNegocioException("El costo unitario no puede ser negativo");
        }
        BigDecimal descuento = d.descuentoPct() == null ? BigDecimal.ZERO : d.descuentoPct();
        if (descuento.signum() < 0 || descuento.compareTo(CIEN) >= 0) {
            throw new ReglaDeNegocioException("El descuento de una línea va de 0 a menos de 100 %");
        }
        String lote = limpiar(d.codigoLote());
        boolean exigeLote = d.manejaLotes() || d.perecedero();
        if (exigeLote && lote == null) {
            throw new ReglaDeNegocioException(
                    "Este producto maneja lotes: indica el código de lote que llegó");
        }
        if (d.perecedero() && d.fechaVencimiento() == null) {
            throw new ReglaDeNegocioException(
                    "Este producto es perecedero: indica la fecha de vencimiento del lote");
        }
        if (exigeLote && d.fechaVencimiento() != null
                && d.fechaVencimiento().isBefore(fechaRecepcion)) {
            throw new ReglaDeNegocioException("El lote " + lote + " venció el "
                    + d.fechaVencimiento() + ": no se recibe mercancía vencida");
        }
        RecepcionLinea l = new RecepcionLinea();
        l.id = UUID.randomUUID();
        l.negocioId = negocioId;
        l.recepcionId = recepcionId;
        l.ordenLineaId = d.ordenLineaId();
        l.productoId = d.productoId();
        l.nombre = limpiar(d.nombre());
        l.codigoProveedor = limpiar(d.codigoProveedor());
        l.cantidad = d.cantidad();
        l.costoUnitario = d.costoUnitario();
        l.descuentoPct = descuento;
        l.tarifaIva = d.tarifaIva();
        l.impuestoPct = d.impuestoPct() == null ? BigDecimal.ZERO : d.impuestoPct();
        // HU-048 C2: si no maneja lotes, no se guarda nada de lote aunque venga.
        l.codigoLote = exigeLote ? lote : null;
        l.fechaVencimiento = exigeLote ? d.fechaVencimiento() : null;
        l.registroSanitario = exigeLote ? limpiar(d.registroSanitario()) : null;
        return l;
    }

    /** Atajo de HU-165: una línea sin descuento ni IVA. */
    public static RecepcionLinea nueva(UUID recepcionId, UUID negocioId, UUID ordenLineaId,
            UUID productoId, BigDecimal cantidad, BigDecimal costoUnitario, boolean manejaLotes,
            boolean perecedero, LocalDate fechaRecepcion, String codigoLote,
            LocalDate fechaVencimiento, String registroSanitario) {
        return nueva(recepcionId, negocioId, new Datos(ordenLineaId, productoId, null, null,
                cantidad, costoUnitario, BigDecimal.ZERO, null, BigDecimal.ZERO, manejaLotes,
                perecedero, codigoLote, fechaVencimiento, registroSanitario), fechaRecepcion);
    }

    /** Costo unitario con el descuento de la línea, sin IVA. */
    public BigDecimal costoNeto() {
        BigDecimal factor = BigDecimal.ONE.subtract(descuentoPct.divide(CIEN, 8, RoundingMode.HALF_UP));
        return costoUnitario.multiply(factor).setScale(4, RoundingMode.HALF_UP);
    }

    /** La base de la línea: cantidad × costo neto, sin IVA. */
    public BigDecimal subtotal() {
        return cantidad.multiply(costoNeto()).setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * El IVA de la línea. Si la recepción ya repartió el descuento global, es el que se calculó
     * sobre la base rebajada; si no, base × tarifa.
     */
    public BigDecimal impuesto() {
        if (impuestoValor != null) {
            return impuestoValor;
        }
        return subtotal().multiply(impuestoPct).divide(CIEN, 4, RoundingMode.HALF_UP);
    }

    /** HU-168 · Lo calcula {@link CostoDeEntrada} al totalizar la recepción. */
    void asignarCosto(BigDecimal costoEntrada, BigDecimal impuesto) {
        this.costoEntrada = costoEntrada;
        this.impuestoValor = impuesto;
    }

    /** El costo con que entra al inventario; antes de totalizar, el costo neto. */
    public BigDecimal getCostoEntrada() {
        return costoEntrada != null ? costoEntrada : costoNeto();
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

    public UUID getRecepcionId() {
        return recepcionId;
    }

    public UUID getOrdenLineaId() {
        return ordenLineaId;
    }

    public UUID getProductoId() {
        return productoId;
    }

    public String getNombre() {
        return nombre;
    }

    public String getCodigoProveedor() {
        return codigoProveedor;
    }

    public BigDecimal getCantidad() {
        return cantidad;
    }

    public BigDecimal getCostoUnitario() {
        return costoUnitario;
    }

    public BigDecimal getDescuentoPct() {
        return descuentoPct;
    }

    public String getTarifaIva() {
        return tarifaIva;
    }

    public BigDecimal getImpuestoPct() {
        return impuestoPct;
    }

    public String getCodigoLote() {
        return codigoLote;
    }

    public LocalDate getFechaVencimiento() {
        return fechaVencimiento;
    }

    public void fijarPrecioVentaNuevo(BigDecimal precio) {
        this.precioVentaNuevo = precio;
    }

    public BigDecimal getPrecioVentaNuevo() {
        return precioVentaNuevo;
    }

    public String getRegistroSanitario() {
        return registroSanitario;
    }
}
