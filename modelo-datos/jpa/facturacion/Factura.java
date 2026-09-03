package com.regenta.facturacion.dominio;

import jakarta.persistence.*;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Agregado raíz de Facturación electrónica.
 *
 * DOS DECISIONES QUE VIENEN DEL ANÁLISIS:
 *
 * 1. ORIGEN POLIMÓRFICO. El PDF dice "Facturación depende de Ventas". Con
 *    tres patrones operativos eso ya no alcanza: también factura Reservas y
 *    Comandas. Por eso `origenTipo` + `origenId` en vez de una FK a `ventas`.
 *
 * 2. SNAPSHOTS INMUTABLES. `emisorSnapshot` y `clienteSnapshot` son JSONB
 *    con los datos tal como estaban al emitir. Una factura es un documento
 *    legal: si mañana el cliente cambia de dirección, la factura de ayer no
 *    puede cambiar. Por eso no hay @ManyToOne a Cliente.
 */
@Entity
@Table(name = "facturas", schema = "facturacion")
public class Factura {

    @Id private UUID id;

    @Column(name = "negocio_id", nullable = false, updatable = false) private UUID negocioId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "resolucion_id", nullable = false)
    private Resolucion resolucion;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_documento", nullable = false, length = 20)
    private TipoDocumento tipoDocumento;

    @Column(name = "prefijo", nullable = false, length = 10) private String prefijo = "";
    @Column(name = "numero", nullable = false) private Long numero;

    @Generated
    @Column(name = "numero_completo", insertable = false, updatable = false)
    private String numeroCompleto;

    @Enumerated(EnumType.STRING)
    @Column(name = "origen_tipo", nullable = false, length = 20)
    private OrigenFactura origenTipo;

    @Column(name = "origen_id") private UUID origenId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "emisor_snapshot", nullable = false, columnDefinition = "jsonb", updatable = false)
    private Map<String, Object> emisorSnapshot;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "cliente_snapshot", nullable = false, columnDefinition = "jsonb", updatable = false)
    private Map<String, Object> clienteSnapshot;

    @Column(name = "fecha_emision", nullable = false) private OffsetDateTime fechaEmision = OffsetDateTime.now();
    @Column(name = "total", nullable = false, precision = 16, scale = 4) private BigDecimal total = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoFactura estado = EstadoFactura.BORRADOR;

    @Column(name = "cufe", length = 96) private String cufe;
    @Column(name = "xml_url") private String xmlUrl;   // el XML va a storage, no a la BD
    @Column(name = "pdf_url") private String pdfUrl;

    @Version @Column(name = "version", nullable = false) private Long version;

    public enum TipoDocumento { FACTURA_VENTA, FACTURA_POS, NOTA_CREDITO, NOTA_DEBITO, DOCUMENTO_SOPORTE }
    public enum OrigenFactura { VENTA, RESERVA, COMANDA, MANUAL, DEVOLUCION }
    public enum EstadoFactura { BORRADOR, GENERADA, FIRMADA, ENVIADA, ACEPTADA, RECHAZADA, ANULADA, CONTINGENCIA }

    /**
     * El consecutivo NO puede ser un BIGSERIAL: la DIAN autoriza un rango por
     * resolución y exige numeración continua dentro de él. Se toma con
     * SELECT ... FOR UPDATE sobre la resolución, dentro de la transacción.
     */
    public void asignarNumero(Resolucion resolucion) {
        if (this.numero != null) throw new IllegalStateException("La factura ya tiene número");
        this.resolucion = resolucion;
        this.prefijo = resolucion.getPrefijo();
        this.numero = resolucion.tomarSiguienteConsecutivo();   // valida el rango
        this.estado = EstadoFactura.GENERADA;
    }

    /** Una factura emitida NUNCA se borra ni se edita: se anula con una nota
     *  crédito que la referencia. */
    public boolean esModificable() { return estado == EstadoFactura.BORRADOR; }

    public void marcarAceptada(String cufe, String xmlUrl, String pdfUrl) {
        this.cufe = cufe; this.xmlUrl = xmlUrl; this.pdfUrl = pdfUrl;
        this.estado = EstadoFactura.ACEPTADA;
    }
}
