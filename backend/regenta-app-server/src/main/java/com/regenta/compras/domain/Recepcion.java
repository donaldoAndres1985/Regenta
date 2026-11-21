package com.regenta.compras.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import com.regenta.shared.web.ConflictoDeEstadoException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * Lo que llegó de un proveedor, con su factura (HU-048, HU-167). Con {@code ordenId} nulo es una
 * compra directa (I13 de {@code docs/dominio/compras.md}).
 *
 * <p>Totales (I22): {@code subtotal} = suma de las bases de las líneas; {@code impuestoTotal} =
 * suma de su IVA; {@code total} = subtotal + IVA + flete. Es lo que se le debe al proveedor.
 *
 * <p>Se arma en {@link EstadoRecepcion#BORRADOR} con las líneas recibidas y sus
 * lotes; al {@link #confirmar()} se sube el inventario y —si trae la factura del
 * proveedor— se abre la cuenta por pagar con el vencimiento del plazo del
 * proveedor (criterio 6).
 */
@Entity
@Table(name = "recepciones", schema = "compras")
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

    @Column(name = "fecha_factura")
    private LocalDate fechaFactura;

    @Column(nullable = false)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "impuesto_total", nullable = false)
    private BigDecimal impuestoTotal = BigDecimal.ZERO;

    @Column(nullable = false)
    private BigDecimal flete = BigDecimal.ZERO;

    /** HU-169 · Contado o crédito (I23, I24). */
    @Enumerated(EnumType.STRING)
    @Column(name = "condicion_pago", nullable = false, length = 10)
    private CondicionDePago condicionPago = CondicionDePago.CREDITO;

    /** HU-169 · Con qué se paga una compra de contado. */
    @Enumerated(EnumType.STRING)
    @Column(name = "metodo_pago", length = 20)
    private MetodoDePago metodoPago;

    /** HU-169 / HU-145 · De contado en efectivo con la plata de la caja abierta del usuario. */
    @Column(name = "pagar_desde_caja", nullable = false)
    private boolean pagarDesdeLaCaja;

    @Column(name = "referencia_pago", length = 60)
    private String referenciaPago;

    /** HU-168 · Descuento a pie de factura; se reparte entre las líneas por su valor. */
    @Column(name = "descuento_global", nullable = false)
    private BigDecimal descuentoGlobal = BigDecimal.ZERO;

    @Column(nullable = false)
    private BigDecimal total;

    @Column(name = "usuario_id")
    private UUID usuarioId;

    @Column(columnDefinition = "text")
    private String observaciones;

    /** HU-177 · Quién, cuándo y por qué se anuló; nulos si no se anuló. */
    @Column(name = "anulada_por")
    private UUID anuladaPor;

    @Column(name = "anulada_en")
    private java.time.OffsetDateTime anuladaEn;

    @Column(name = "motivo_anulacion", length = 200)
    private String motivoAnulacion;

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

    /** Totaliza como un negocio responsable de IVA. */
    public void totalizar(List<RecepcionLinea> lineas) {
        totalizar(lineas, true);
    }

    /**
     * HU-168 · Reparte descuento global y flete, fija el costo de entrada de cada línea (I14) y deja
     * los totales: {@code subtotal} = suma de netos; {@code impuestoTotal} = IVA sobre la base ya
     * rebajada; {@code total} = subtotal − descuento global + IVA + flete.
     */
    public void totalizar(List<RecepcionLinea> lineas, boolean responsableDeIva) {
        BigDecimal netos = BigDecimal.ZERO;
        List<CostoDeEntrada.Entrada> entradas = new java.util.ArrayList<>();
        for (RecepcionLinea l : lineas) {
            netos = netos.add(l.subtotal());
            entradas.add(new CostoDeEntrada.Entrada(l.getCantidad(), l.subtotal(), l.getImpuestoPct()));
        }
        if (descuentoGlobal.compareTo(netos) > 0) {
            throw new com.regenta.shared.web.ReglaDeNegocioException("El descuento global ("
                    + descuentoGlobal + ") no puede ser mayor que el valor de la factura (" + netos + ")");
        }
        List<CostoDeEntrada.Resultado> costos = CostoDeEntrada.calcular(entradas, descuentoGlobal,
                flete, responsableDeIva);
        BigDecimal iva = BigDecimal.ZERO;
        for (int i = 0; i < lineas.size(); i++) {
            lineas.get(i).asignarCosto(costos.get(i).costoUnitario(), costos.get(i).iva());
            iva = iva.add(costos.get(i).iva());
        }
        this.subtotal = netos.setScale(4, RoundingMode.HALF_UP);
        this.impuestoTotal = iva.setScale(4, RoundingMode.HALF_UP);
        this.total = subtotal.subtract(descuentoGlobal).add(impuestoTotal).add(flete)
                .setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * HU-167 · Los datos de la factura del proveedor: número, fecha y flete. Solo en borrador: una
     * recepción confirmada no se edita (I17).
     */
    public void datosDeFactura(String numero, LocalDate fecha, BigDecimal flete,
            BigDecimal descuentoGlobal, String observaciones) {
        exigirBorrador();
        if (flete != null && flete.signum() < 0) {
            throw new com.regenta.shared.web.ReglaDeNegocioException("El flete no puede ser negativo");
        }
        if (descuentoGlobal != null && descuentoGlobal.signum() < 0) {
            throw new com.regenta.shared.web.ReglaDeNegocioException(
                    "El descuento global no puede ser negativo");
        }
        this.descuentoGlobal = descuentoGlobal == null ? BigDecimal.ZERO : descuentoGlobal;
        this.facturaProveedor = limpiar(numero);
        this.fechaFactura = fecha;
        this.flete = flete == null ? BigDecimal.ZERO : flete;
        this.observaciones = limpiar(observaciones);
    }

    /**
     * HU-169 · Cómo se paga la factura. A crédito no lleva método. De contado el método es
     * obligatorio, y solo el efectivo puede salir de la caja (I24, I29).
     */
    public void condicionDePago(CondicionDePago condicion, MetodoDePago metodo,
            boolean desdeLaCaja, String referencia) {
        exigirBorrador();
        CondicionDePago c = condicion == null ? CondicionDePago.CREDITO : condicion;
        if (c == CondicionDePago.CONTADO && metodo == null) {
            throw new com.regenta.shared.web.ReglaDeNegocioException(
                    "Indica con qué se pagó la compra de contado");
        }
        if (c == CondicionDePago.CONTADO && desdeLaCaja && metodo != MetodoDePago.EFECTIVO) {
            throw new com.regenta.shared.web.ReglaDeNegocioException(
                    "Solo un pago en efectivo sale de la caja");
        }
        this.condicionPago = c;
        this.metodoPago = c == CondicionDePago.CONTADO ? metodo : null;
        this.pagarDesdeLaCaja = c == CondicionDePago.CONTADO && desdeLaCaja;
        this.referenciaPago = c == CondicionDePago.CONTADO ? limpiar(referencia) : null;
    }

    public boolean esDeContado() {
        return condicionPago == CondicionDePago.CONTADO;
    }

    /** I17: una recepción confirmada o anulada no se edita (409). */
    public void exigirBorrador() {
        if (estado != EstadoRecepcion.BORRADOR) {
            throw new ConflictoDeEstadoException(
                    "Esta recepción ya está " + estado + ": no se edita. Se anula o se devuelve.");
        }
    }

    public boolean esCompraDirecta() {
        return ordenId == null;
    }

    /** BORRADOR → CONFIRMADA. Confirmar dos veces es un conflicto de estado. */
    /**
     * HU-176 · Una recepción que se estaba armando y ya no va, porque su orden se anuló o se cerró.
     * Solo en borrador: lo confirmado no se descarta.
     */
    public void descartar() {
        exigirBorrador();
        this.estado = EstadoRecepcion.ANULADA;
    }

    /**
     * HU-177 · Anula una recepción confirmada, con motivo. Sigue visible (C5); quien la llama ya
     * revisó que no tenga abonos y que la mercancía siga en la bodega.
     */
    public void anular(UUID usuario, String motivo) {
        if (estado != EstadoRecepcion.CONFIRMADA) {
            throw new ConflictoDeEstadoException("Se anula una recepción confirmada; esta está " + estado);
        }
        String limpio = motivo == null ? "" : motivo.trim();
        if (limpio.isEmpty()) {
            throw new com.regenta.shared.web.ReglaDeNegocioException(
                    "La anulación de una recepción exige un motivo");
        }
        this.motivoAnulacion = limpio.length() > 200 ? limpio.substring(0, 200) : limpio;
        this.anuladaPor = usuario;
        this.anuladaEn = java.time.OffsetDateTime.now();
        this.estado = EstadoRecepcion.ANULADA;
    }

    public UUID getAnuladaPor() {
        return anuladaPor;
    }

    public java.time.OffsetDateTime getAnuladaEn() {
        return anuladaEn;
    }

    public String getMotivoAnulacion() {
        return motivoAnulacion;
    }

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

    /**
     * HU-170 · Llegó la factura de una recepción que vino con remisión. A diferencia de
     * {@link #datosDeFactura}, vale aunque la recepción ya esté confirmada: solo completa el número
     * y la fecha, no cambia lo recibido.
     */
    public void facturaRecibida(String numero, LocalDate fecha) {
        if (tieneFactura()) {
            throw new ConflictoDeEstadoException(
                    "Esta recepción ya tiene la factura " + facturaProveedor + " registrada");
        }
        this.facturaProveedor = limpiar(numero);
        this.fechaFactura = fecha;
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

    public LocalDate getFechaFactura() {
        return fechaFactura;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public BigDecimal getImpuestoTotal() {
        return impuestoTotal;
    }

    public BigDecimal getFlete() {
        return flete;
    }

    public BigDecimal getDescuentoGlobal() {
        return descuentoGlobal;
    }

    public CondicionDePago getCondicionPago() {
        return condicionPago;
    }

    public MetodoDePago getMetodoPago() {
        return metodoPago;
    }

    public boolean isPagarDesdeLaCaja() {
        return pagarDesdeLaCaja;
    }

    public String getReferenciaPago() {
        return referenciaPago;
    }
}
