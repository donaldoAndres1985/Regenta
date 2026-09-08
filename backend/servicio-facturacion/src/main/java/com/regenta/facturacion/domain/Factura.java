package com.regenta.facturacion.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import com.regenta.comun.errores.ReglaDeNegocioException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;

/**
 * Una factura electrónica (HU-053). Nace de un evento de cierre —de una venta,
 * una estancia o un pedido— y no de una tabla: el origen es polimórfico
 * ({@code origenTipo} + {@code origenId}).
 *
 * <p>{@code emisorSnapshot} y {@code clienteSnapshot} son JSONB inmutables: una
 * factura emitida no cambia si mañana editan el cliente o la configuración del
 * negocio (criterios 5 y 6).
 */
@Entity
@Table(name = "facturas")
public class Factura {

    private static final int ESCALA = 4;

    @Id
    private UUID id;

    @Column(name = "negocio_id", nullable = false, updatable = false)
    private UUID negocioId;

    @Column(name = "sucursal_id")
    private UUID sucursalId;

    @Column(name = "resolucion_id", nullable = false)
    private UUID resolucionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_documento", nullable = false, length = 20)
    private TipoDocumento tipoDocumento;

    @Column(nullable = false, length = 10)
    private String prefijo;

    @Column(nullable = false)
    private long numero;

    @Column(name = "numero_completo", length = 30, insertable = false, updatable = false)
    private String numeroCompleto;

    @Enumerated(EnumType.STRING)
    @Column(name = "origen_tipo", nullable = false, length = 20)
    private OrigenDeFactura origenTipo;

    @Column(name = "origen_id")
    private UUID origenId;

    @Column(name = "factura_origen_id")
    private UUID facturaOrigenId;

    @Column(name = "codigo_nota", length = 10)
    private String codigoNota;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "emisor_snapshot", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> emisorSnapshot;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "cliente_snapshot", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> clienteSnapshot;

    @Column(name = "cliente_id")
    private UUID clienteId;

    @Column(name = "fecha_emision", nullable = false)
    private OffsetDateTime fechaEmision;

    @Column(name = "fecha_vencimiento")
    private LocalDate fechaVencimiento;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(nullable = false, length = 3)
    private String moneda;

    @Column(name = "tasa_cambio", nullable = false)
    private BigDecimal tasaCambio;

    @Column(nullable = false)
    private BigDecimal subtotal;

    @Column(name = "descuento_total", nullable = false)
    private BigDecimal descuentoTotal;

    @Column(name = "base_gravable", nullable = false)
    private BigDecimal baseGravable;

    @Column(name = "impuestos_total", nullable = false)
    private BigDecimal impuestosTotal;

    @Column(name = "retenciones_total", nullable = false)
    private BigDecimal retencionesTotal;

    @Column(nullable = false)
    private BigDecimal propina;

    @Column(nullable = false)
    private BigDecimal total;

    @Enumerated(EnumType.STRING)
    @Column(name = "forma_pago", nullable = false, length = 20)
    private FormaPago formaPago;

    @Column(name = "medio_pago_codigo", length = 10)
    private String medioPagoCodigo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoFactura estado;

    @Column(length = 96)
    private String cufe;

    @Column(name = "xml_url", columnDefinition = "text")
    private String xmlUrl;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "respuesta_dian", columnDefinition = "jsonb")
    private Map<String, Object> respuestaDian;

    @Column(name = "intentos_envio", nullable = false)
    private short intentosEnvio;

    @Column(name = "enviada_en")
    private OffsetDateTime enviadaEn;

    @Column(name = "aceptada_en")
    private OffsetDateTime aceptadaEn;

    @CreationTimestamp
    @Column(name = "creado_en", nullable = false, updatable = false)
    private OffsetDateTime creadoEn;

    @UpdateTimestamp
    @Column(name = "actualizado_en", nullable = false)
    private OffsetDateTime actualizadoEn;

    @Version
    @Column(nullable = false)
    private long version;

    @Transient
    private final List<FacturaLinea> lineas = new ArrayList<>();

    @Transient
    private final List<FacturaImpuesto> impuestos = new ArrayList<>();

    protected Factura() {
    }

    /**
     * Arma la factura a partir de los datos de un cierre. Recalcula los
     * totales de cabecera desde las líneas: son la única fuente de verdad.
     */
    public static Factura emitir(UUID negocioId, UUID sucursalId, UUID resolucionId,
            TipoDocumento tipoDocumento, String prefijo, long numero, OrigenDeFactura origenTipo,
            UUID origenId, Map<String, Object> emisorSnapshot, Map<String, Object> clienteSnapshot,
            UUID clienteId, String moneda, BigDecimal tasaCambio, FormaPago formaPago,
            String medioPagoCodigo, LocalDate fechaVencimiento, BigDecimal propina,
            List<LineaFacturable> lineasFacturables) {
        return armar(negocioId, sucursalId, resolucionId, tipoDocumento, prefijo, numero, origenTipo,
                origenId, null, null, emisorSnapshot, clienteSnapshot, clienteId, moneda, tasaCambio,
                formaPago, medioPagoCodigo, fechaVencimiento, propina, lineasFacturables);
    }

    /**
     * Emite una nota crédito contra una factura de origen (HU-056): mismo cálculo
     * de líneas, pero {@code tipoDocumento = NOTA_CREDITO}, con
     * {@code facturaOrigenId} y el {@code codigoNota} de motivo DIAN.
     */
    public static Factura emitirNotaCredito(UUID negocioId, UUID sucursalId, UUID resolucionId,
            String prefijo, long numero, OrigenDeFactura origenTipo, UUID origenId,
            UUID facturaOrigenId, String codigoNota, Map<String, Object> emisorSnapshot,
            Map<String, Object> clienteSnapshot, UUID clienteId, String moneda, FormaPago formaPago,
            List<LineaFacturable> lineasFacturables) {
        if (facturaOrigenId == null) {
            throw new ReglaDeNegocioException(
                    "Una nota crédito tiene que referenciar la factura de origen");
        }
        return armar(negocioId, sucursalId, resolucionId, TipoDocumento.NOTA_CREDITO, prefijo,
                numero, origenTipo, origenId, facturaOrigenId, codigoNota, emisorSnapshot,
                clienteSnapshot, clienteId, moneda, BigDecimal.ONE, formaPago, null, null,
                BigDecimal.ZERO, lineasFacturables);
    }

    private static Factura armar(UUID negocioId, UUID sucursalId, UUID resolucionId,
            TipoDocumento tipoDocumento, String prefijo, long numero, OrigenDeFactura origenTipo,
            UUID origenId, UUID facturaOrigenId, String codigoNota,
            Map<String, Object> emisorSnapshot, Map<String, Object> clienteSnapshot, UUID clienteId,
            String moneda, BigDecimal tasaCambio, FormaPago formaPago, String medioPagoCodigo,
            LocalDate fechaVencimiento, BigDecimal propina,
            List<LineaFacturable> lineasFacturables) {
        if (lineasFacturables == null || lineasFacturables.isEmpty()) {
            throw new ReglaDeNegocioException("El documento no trae líneas");
        }
        Factura f = new Factura();
        f.id = UUID.randomUUID();
        f.negocioId = negocioId;
        f.sucursalId = sucursalId;
        f.resolucionId = resolucionId;
        f.tipoDocumento = tipoDocumento;
        f.prefijo = prefijo == null ? "" : prefijo;
        f.numero = numero;
        f.origenTipo = origenTipo;
        f.origenId = origenId;
        f.facturaOrigenId = facturaOrigenId;
        f.codigoNota = codigoNota;
        f.emisorSnapshot = emisorSnapshot == null ? new LinkedHashMap<>() : emisorSnapshot;
        f.clienteSnapshot = clienteSnapshot == null ? new LinkedHashMap<>() : clienteSnapshot;
        f.clienteId = clienteId;
        f.fechaEmision = OffsetDateTime.now();
        f.fechaVencimiento = fechaVencimiento;
        f.moneda = moneda == null || moneda.isBlank() ? "COP" : moneda;
        f.tasaCambio = tasaCambio == null ? BigDecimal.ONE : tasaCambio;
        f.formaPago = formaPago == null ? FormaPago.CONTADO : formaPago;
        f.medioPagoCodigo = medioPagoCodigo;
        f.propina = escala(propina == null ? BigDecimal.ZERO : propina);
        f.estado = EstadoFactura.GENERADA;
        f.intentosEnvio = 0;

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal descuentoTotal = BigDecimal.ZERO;
        BigDecimal base = BigDecimal.ZERO;
        BigDecimal impuestos = BigDecimal.ZERO;
        BigDecimal retenciones = BigDecimal.ZERO;

        short indice = 1;
        for (LineaFacturable lf : lineasFacturables) {
            BigDecimal cantidad = nz(lf.cantidad());
            BigDecimal precio = nz(lf.precioUnitario());
            BigDecimal bruto = escala(cantidad.multiply(precio));
            BigDecimal descPct = nz(lf.descuentoPct());
            BigDecimal descValor = lf.descuentoValor() != null
                    ? escala(lf.descuentoValor())
                    : escala(bruto.multiply(descPct).movePointLeft(2));
            BigDecimal bg = lf.baseGravable() != null
                    ? escala(lf.baseGravable())
                    : escala(bruto.subtract(descValor));

            FacturaLinea linea = FacturaLinea.de(negocioId, f.id, indice, lf.codigo(),
                    lf.descripcion(), cantidad, lf.unidadCodigo(), precio, descPct, descValor, bg,
                    BigDecimal.ZERO);
            f.lineas.add(linea);

            BigDecimal impDeLinea = BigDecimal.ZERO;
            BigDecimal retDeLinea = BigDecimal.ZERO;
            for (LineaFacturable.ImpuestoFacturable imp : lf.impuestos()) {
                BigDecimal impBase = imp.base() != null ? escala(imp.base()) : bg;
                BigDecimal impValor = imp.valor() != null
                        ? escala(imp.valor())
                        : escala(impBase.multiply(nz(imp.porcentaje())).movePointLeft(2));
                f.impuestos.add(FacturaImpuesto.de(negocioId, f.id, linea.getId(), imp.codigo(),
                        imp.nombre(), nz(imp.porcentaje()), impBase, impValor, imp.esRetencion()));
                if (imp.esRetencion()) {
                    retDeLinea = retDeLinea.add(impValor);
                } else {
                    impDeLinea = impDeLinea.add(impValor);
                }
            }

            BigDecimal totalLinea = lf.total() != null
                    ? escala(lf.total())
                    : escala(bg.add(impDeLinea).subtract(retDeLinea));
            linea.fijarTotal(totalLinea);

            subtotal = subtotal.add(bruto);
            descuentoTotal = descuentoTotal.add(descValor);
            base = base.add(bg);
            impuestos = impuestos.add(impDeLinea);
            retenciones = retenciones.add(retDeLinea);
            indice++;
        }

        f.subtotal = escala(subtotal);
        f.descuentoTotal = escala(descuentoTotal);
        f.baseGravable = escala(base);
        f.impuestosTotal = escala(impuestos);
        f.retencionesTotal = escala(retenciones);
        f.total = escala(base.add(impuestos).subtract(retenciones).add(f.propina));
        return f;
    }

    /** Criterio 1: se guarda el CUFE y la URL del XML; el XML no va a la base. */
    public void firmar(String cufe, String xmlUrl) {
        if (estado != EstadoFactura.GENERADA) {
            return;   // ya firmada: idempotente
        }
        this.cufe = cufe;
        this.xmlUrl = xmlUrl;
        this.estado = EstadoFactura.FIRMADA;
    }

    public boolean estaFirmada() {
        return cufe != null && estado != EstadoFactura.GENERADA && estado != EstadoFactura.BORRADOR;
    }

    /** Cada intento de transmisión, haya respondido o no la DIAN (criterio 4). */
    public void registrarIntentoDeEnvio() {
        this.intentosEnvio++;
        this.enviadaEn = OffsetDateTime.now();
        if (estado == EstadoFactura.FIRMADA) {
            this.estado = EstadoFactura.ENVIADA;
        }
    }

    public void aceptar(Map<String, Object> respuesta) {
        this.respuestaDian = respuesta;
        this.estado = EstadoFactura.ACEPTADA;
        this.aceptadaEn = OffsetDateTime.now();
    }

    /** Criterio 3: la factura queda RECHAZADA con el código de error. */
    public void rechazar(String codigo, String mensaje) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("codigo", codigo);
        r.put("mensaje", mensaje);
        this.respuestaDian = r;
        this.estado = EstadoFactura.RECHAZADA;
    }

    /**
     * HU-057 criterio 2: se emitió durante una contingencia de la DIAN. Tiene
     * CUFE (ya está firmada), así que se entrega al cliente; se transmitirá al
     * cerrar la contingencia.
     */
    public void marcarContingencia() {
        this.estado = EstadoFactura.CONTINGENCIA;
    }

    public boolean enContingencia() {
        return estado == EstadoFactura.CONTINGENCIA;
    }

    public boolean puedeTransmitirse() {
        return estado == EstadoFactura.FIRMADA || estado == EstadoFactura.ENVIADA
                || estado == EstadoFactura.RECHAZADA || estado == EstadoFactura.CONTINGENCIA;
    }

    private static BigDecimal escala(BigDecimal v) {
        return (v == null ? BigDecimal.ZERO : v).setScale(ESCALA, RoundingMode.HALF_UP);
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    public UUID getId() {
        return id;
    }

    public UUID getNegocioId() {
        return negocioId;
    }

    public UUID getResolucionId() {
        return resolucionId;
    }

    public String getPrefijo() {
        return prefijo;
    }

    public long getNumero() {
        return numero;
    }

    public String getNumeroCompleto() {
        return numeroCompleto != null ? numeroCompleto : prefijo + numero;
    }

    public TipoDocumento getTipoDocumento() {
        return tipoDocumento;
    }

    public OrigenDeFactura getOrigenTipo() {
        return origenTipo;
    }

    public UUID getOrigenId() {
        return origenId;
    }

    public UUID getFacturaOrigenId() {
        return facturaOrigenId;
    }

    public String getCodigoNota() {
        return codigoNota;
    }

    public UUID getSucursalId() {
        return sucursalId;
    }

    public Map<String, Object> getEmisorSnapshot() {
        return emisorSnapshot;
    }

    public Map<String, Object> getClienteSnapshot() {
        return clienteSnapshot;
    }

    public UUID getClienteId() {
        return clienteId;
    }

    public OffsetDateTime getFechaEmision() {
        return fechaEmision;
    }

    public LocalDate getFechaVencimiento() {
        return fechaVencimiento;
    }

    public String getMoneda() {
        return moneda;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public BigDecimal getDescuentoTotal() {
        return descuentoTotal;
    }

    public BigDecimal getBaseGravable() {
        return baseGravable;
    }

    public BigDecimal getImpuestosTotal() {
        return impuestosTotal;
    }

    public BigDecimal getRetencionesTotal() {
        return retencionesTotal;
    }

    public BigDecimal getPropina() {
        return propina;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public FormaPago getFormaPago() {
        return formaPago;
    }

    public EstadoFactura getEstado() {
        return estado;
    }

    public String getCufe() {
        return cufe;
    }

    public String getXmlUrl() {
        return xmlUrl;
    }

    public Map<String, Object> getRespuestaDian() {
        return respuestaDian;
    }

    public short getIntentosEnvio() {
        return intentosEnvio;
    }

    public List<FacturaLinea> getLineas() {
        return lineas;
    }

    public List<FacturaImpuesto> getImpuestos() {
        return impuestos;
    }
}
