package com.regenta.comandas.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * La cabecera de una comanda (HU-085). La transacción del patrón Comanda: queda
 * abierta y acumula líneas durante el servicio. Los totales se recalculan en
 * cada adición y se persisten (criterio 3). Solo una comanda abierta por sesión
 * de mesa (criterio 4, respaldado por {@code uq_comanda_sesion_abierta}).
 */
@Entity
@Table(name = "comandas")
public class Comanda {

    @Id
    private UUID id;

    @Column(name = "negocio_id", nullable = false, updatable = false)
    private UUID negocioId;

    @Column(name = "sucursal_id")
    private UUID sucursalId;

    @Column(nullable = false, length = 30, updatable = false)
    private String numero;

    @Column(nullable = false, length = 20)
    private String tipo;

    @Column(name = "mesa_id")
    private UUID mesaId;

    @Column(name = "sesion_mesa_id")
    private UUID sesionMesaId;

    @Column(name = "mesero_usuario_id")
    private UUID meseroUsuarioId;

    @Column(name = "num_comensales", nullable = false)
    private short numComensales;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoDeComanda estado;

    @Column(nullable = false)
    private BigDecimal subtotal;

    @Column(name = "descuento_total", nullable = false)
    private BigDecimal descuentoTotal;

    @Column(name = "impuesto_total", nullable = false)
    private BigDecimal impuestoTotal;

    @Column(name = "propina_sugerida", nullable = false)
    private BigDecimal propinaSugerida;

    @Column(nullable = false)
    private BigDecimal propina;

    @Column(nullable = false)
    private BigDecimal total;

    @Column(name = "costo_total", nullable = false)
    private BigDecimal costoTotal;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(nullable = false, length = 3, columnDefinition = "char(3)")
    private String moneda;

    @Column(name = "abierta_en", nullable = false, updatable = false)
    private OffsetDateTime abiertaEn;

    @Column(name = "enviada_cocina_en")
    private OffsetDateTime enviadaCocinaEn;

    @Column(name = "notas", columnDefinition = "text")
    private String notas;

    @CreationTimestamp
    @Column(name = "creado_en", nullable = false, updatable = false)
    private OffsetDateTime creadoEn;

    @UpdateTimestamp
    @Column(name = "actualizado_en", nullable = false)
    private OffsetDateTime actualizadoEn;

    @Version
    @Column(nullable = false)
    private long version;

    protected Comanda() {
    }

    public static Comanda abrir(UUID negocioId, String numero, UUID mesaId, UUID sesionMesaId,
            UUID meseroUsuarioId, Integer numComensales, String notas) {
        Comanda c = new Comanda();
        c.id = UUID.randomUUID();
        c.negocioId = negocioId;
        c.numero = numero;
        c.tipo = "MESA";
        c.mesaId = mesaId;
        c.sesionMesaId = sesionMesaId;
        c.meseroUsuarioId = meseroUsuarioId;
        c.numComensales = (short) Math.max(1, numComensales == null ? 1 : numComensales);
        c.estado = EstadoDeComanda.ABIERTA;
        c.moneda = "COP";
        c.abiertaEn = OffsetDateTime.now();
        c.notas = notas == null || notas.isBlank() ? null : notas.trim();
        c.subtotal = BigDecimal.ZERO;
        c.descuentoTotal = BigDecimal.ZERO;
        c.impuestoTotal = BigDecimal.ZERO;
        c.propinaSugerida = BigDecimal.ZERO;
        c.propina = BigDecimal.ZERO;
        c.total = BigDecimal.ZERO;
        c.costoTotal = BigDecimal.ZERO;
        return c;
    }

    /** Recalcula los totales desde las líneas vivas (HU-085 criterio 3). */
    public void recalcular(List<ComandaLinea> lineas) {
        BigDecimal sub = BigDecimal.ZERO;
        BigDecimal imp = BigDecimal.ZERO;
        BigDecimal desc = BigDecimal.ZERO;
        BigDecimal costo = BigDecimal.ZERO;
        BigDecimal tot = BigDecimal.ZERO;
        for (ComandaLinea l : lineas) {
            if (!l.cuenta()) {
                continue;
            }
            sub = sub.add(l.getSubtotal());
            imp = imp.add(l.getImpuestoValor());
            desc = desc.add(l.getDescuentoValor());
            costo = costo.add(l.costoTotal());
            tot = tot.add(l.getTotal());
        }
        this.subtotal = escala4(sub);
        this.impuestoTotal = escala4(imp);
        this.descuentoTotal = escala4(desc);
        this.costoTotal = escala4(costo);
        this.total = escala4(tot);
        this.propinaSugerida = escala4(sub.multiply(new BigDecimal("0.10")));
    }

    /** Al enviar a cocina por primera vez (HU-085). */
    public void marcarEnCocina() {
        if (estado == EstadoDeComanda.ABIERTA) {
            this.estado = EstadoDeComanda.EN_COCINA;
        }
        if (enviadaCocinaEn == null) {
            this.enviadaCocinaEn = OffsetDateTime.now();
        }
    }

    /** Cuando todas las líneas vivas quedaron entregadas (HU-086). */
    public void marcarServida() {
        if (estado == EstadoDeComanda.EN_COCINA || estado == EstadoDeComanda.ABIERTA) {
            this.estado = EstadoDeComanda.SERVIDA;
        }
    }

    private static BigDecimal escala4(BigDecimal v) {
        return (v == null ? BigDecimal.ZERO : v).setScale(4, RoundingMode.HALF_UP);
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

    public String getTipo() {
        return tipo;
    }

    public UUID getMesaId() {
        return mesaId;
    }

    public UUID getSesionMesaId() {
        return sesionMesaId;
    }

    public UUID getMeseroUsuarioId() {
        return meseroUsuarioId;
    }

    public short getNumComensales() {
        return numComensales;
    }

    public EstadoDeComanda getEstado() {
        return estado;
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

    public BigDecimal getPropinaSugerida() {
        return propinaSugerida;
    }

    public BigDecimal getPropina() {
        return propina;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public BigDecimal getCostoTotal() {
        return costoTotal;
    }

    public String getMoneda() {
        return moneda == null ? "COP" : moneda.trim();
    }

    public OffsetDateTime getAbiertaEn() {
        return abiertaEn;
    }

    public OffsetDateTime getEnviadaCocinaEn() {
        return enviadaCocinaEn;
    }

    public String getNotas() {
        return notas;
    }

    public long getVersion() {
        return version;
    }
}
