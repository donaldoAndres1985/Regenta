package com.regenta.comandas.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.regenta.comun.errores.ReglaDeNegocioException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Una línea de la comanda (HU-085). Al crearse guarda copia del nombre y el
 * precio del ítem en ese momento (criterio 2). Tiene su propio ciclo de vida
 * (HU-086): una línea agregada a una comanda ya enviada nace {@code PENDIENTE}
 * igual (HU-085 criterio 1).
 */
@Entity
@Table(name = "comanda_lineas")
public class ComandaLinea {

    @Id
    private UUID id;

    @Column(name = "negocio_id", nullable = false, updatable = false)
    private UUID negocioId;

    @Column(name = "comanda_id", nullable = false, updatable = false)
    private UUID comandaId;

    /** Nº de línea dentro de la comanda (1, 2, 3…). */
    @Column(nullable = false)
    private short linea;

    @Column(name = "item_menu_id", nullable = false, updatable = false)
    private UUID itemMenuId;

    @Column(name = "nombre_snapshot", nullable = false, length = 150, updatable = false)
    private String nombreSnapshot;

    @Column(name = "estacion_id")
    private UUID estacionId;

    @Column(nullable = false)
    private BigDecimal cantidad;

    @Column(name = "precio_unitario", nullable = false, updatable = false)
    private BigDecimal precioUnitario;

    @Column(name = "descuento_valor", nullable = false)
    private BigDecimal descuentoValor;

    @Column(name = "impuesto_pct", nullable = false)
    private BigDecimal impuestoPct;

    @Column(name = "impuesto_valor", nullable = false)
    private BigDecimal impuestoValor;

    @Column(name = "modificadores_valor", nullable = false)
    private BigDecimal modificadoresValor;

    @Column(nullable = false)
    private BigDecimal subtotal;

    @Column(nullable = false)
    private BigDecimal total;

    @Column(name = "costo_snapshot", nullable = false, updatable = false)
    private BigDecimal costoSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoDeLinea estado;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CursoDeComanda curso;

    @Column(name = "secuencia_envio", nullable = false)
    private short secuenciaEnvio;

    @Column(length = 200)
    private String notas;

    @Column(name = "comensal_numero")
    private Short comensalNumero;

    @Column(name = "pedida_en", nullable = false, updatable = false)
    private OffsetDateTime pedidaEn;

    @Column(name = "enviada_en")
    private OffsetDateTime enviadaEn;

    @Column(name = "lista_en")
    private OffsetDateTime listaEn;

    @Column(name = "entregada_en")
    private OffsetDateTime entregadaEn;

    @Column(name = "anulada_en")
    private OffsetDateTime anuladaEn;

    @Column(name = "anulada_por")
    private UUID anuladaPor;

    @Column(name = "motivo_anulacion", length = 120)
    private String motivoAnulacion;

    @Column(name = "genera_merma", nullable = false)
    private boolean generaMerma;

    protected ComandaLinea() {
    }

    public static ComandaLinea crear(UUID negocioId, UUID comandaId, short linea, UUID itemMenuId,
            String nombre, BigDecimal precioUnitario, UUID estacionId, CursoDeComanda curso,
            BigDecimal costoUnitario, BigDecimal cantidad, BigDecimal modificadoresValor,
            BigDecimal descuentoValor, BigDecimal impuestoPct, String notas, Short comensalNumero,
            Integer secuenciaEnvio) {
        if (cantidad == null || cantidad.signum() <= 0) {
            throw new ReglaDeNegocioException("La cantidad de la línea debe ser mayor que cero");
        }
        ComandaLinea l = new ComandaLinea();
        l.id = UUID.randomUUID();
        l.negocioId = negocioId;
        l.comandaId = comandaId;
        l.linea = linea;
        l.itemMenuId = itemMenuId;
        l.nombreSnapshot = nombre;
        l.precioUnitario = escala4(precioUnitario);
        l.estacionId = estacionId;
        l.curso = curso == null ? CursoDeComanda.FUERTE : curso;
        l.costoSnapshot = escala4(costoUnitario);
        l.cantidad = cantidad;
        l.modificadoresValor = escala4(modificadoresValor);
        l.descuentoValor = escala4(descuentoValor);
        l.impuestoPct = impuestoPct == null ? BigDecimal.ZERO : impuestoPct;
        l.notas = notas == null || notas.isBlank() ? null : notas.trim();
        l.comensalNumero = comensalNumero;
        l.secuenciaEnvio = secuenciaEnvio == null ? (short) 1 : secuenciaEnvio.shortValue();
        l.estado = EstadoDeLinea.PENDIENTE;
        l.pedidaEn = OffsetDateTime.now();
        l.recomputar();
        return l;
    }

    public void cambiarCantidad(BigDecimal cantidad) {
        if (estado.yaEnviada()) {
            throw new ReglaDeNegocioException("La línea ya se envió a cocina: no se cambia");
        }
        if (cantidad == null || cantidad.signum() <= 0) {
            throw new ReglaDeNegocioException("La cantidad de la línea debe ser mayor que cero");
        }
        this.cantidad = cantidad;
        recomputar();
    }

    /** PENDIENTE -> ENVIADA (HU-085 «enviar a cocina»). */
    public void enviar() {
        if (estado != EstadoDeLinea.PENDIENTE) {
            return;
        }
        this.estado = EstadoDeLinea.ENVIADA;
        this.enviadaEn = OffsetDateTime.now();
    }

    /**
     * Avanza al siguiente estado del ciclo (HU-086 criterio 1) y deja su marca de
     * tiempo para medir la demora (criterio 3). No se avanza una línea anulada ni
     * más allá de ENTREGADA.
     */
    public void avanzar() {
        if (estado == EstadoDeLinea.ANULADA) {
            throw new ReglaDeNegocioException("Una línea anulada no avanza");
        }
        EstadoDeLinea siguiente = estado.siguiente();
        this.estado = siguiente;
        OffsetDateTime ahora = OffsetDateTime.now();
        switch (siguiente) {
            case ENVIADA -> this.enviadaEn = ahora;
            case LISTA -> this.listaEn = ahora;
            case ENTREGADA -> this.entregadaEn = ahora;
            default -> {
                // EN_PREPARACION no tiene columna propia de tiempo.
            }
        }
    }

    /** Cambia el curso y la secuencia de envío antes de mandar la línea (HU-086 criterio 4). */
    public void ajustarEnvio(CursoDeComanda curso, Integer secuenciaEnvio) {
        if (estado != EstadoDeLinea.PENDIENTE) {
            throw new ReglaDeNegocioException(
                    "La línea ya se envió a cocina: no se cambia el curso");
        }
        if (curso != null) {
            this.curso = curso;
        }
        if (secuenciaEnvio != null) {
            this.secuenciaEnvio = (short) Math.max(1, secuenciaEnvio);
        }
    }

    /** Minutos entre que se envió y que quedó lista (HU-086 criterio 3). {@code null} si no aplica. */
    public Integer demoraPreparacionMin() {
        if (enviadaEn == null || listaEn == null) {
            return null;
        }
        return (int) Math.max(0, java.time.temporal.ChronoUnit.MINUTES.between(enviadaEn, listaEn));
    }

    private void recomputar() {
        BigDecimal base = precioUnitario.multiply(cantidad).add(modificadoresValor);
        this.subtotal = escala4(base.subtract(descuentoValor));
        this.impuestoValor = escala4(subtotal.multiply(impuestoPct));
        this.total = escala4(subtotal.add(impuestoValor));
    }

    private static BigDecimal escala4(BigDecimal v) {
        return (v == null ? BigDecimal.ZERO : v).setScale(4, RoundingMode.HALF_UP);
    }

    public BigDecimal costoTotal() {
        return escala4(costoSnapshot.multiply(cantidad));
    }

    public boolean cuenta() {
        return estado != EstadoDeLinea.ANULADA;
    }

    public UUID getId() {
        return id;
    }

    public UUID getNegocioId() {
        return negocioId;
    }

    public UUID getComandaId() {
        return comandaId;
    }

    public short getLinea() {
        return linea;
    }

    public UUID getItemMenuId() {
        return itemMenuId;
    }

    public String getNombreSnapshot() {
        return nombreSnapshot;
    }

    public UUID getEstacionId() {
        return estacionId;
    }

    public BigDecimal getCantidad() {
        return cantidad;
    }

    public BigDecimal getPrecioUnitario() {
        return precioUnitario;
    }

    public BigDecimal getModificadoresValor() {
        return modificadoresValor;
    }

    public BigDecimal getDescuentoValor() {
        return descuentoValor;
    }

    public BigDecimal getImpuestoValor() {
        return impuestoValor;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public BigDecimal getCostoSnapshot() {
        return costoSnapshot;
    }

    public EstadoDeLinea getEstado() {
        return estado;
    }

    public CursoDeComanda getCurso() {
        return curso;
    }

    public short getSecuenciaEnvio() {
        return secuenciaEnvio;
    }

    public String getNotas() {
        return notas;
    }

    public Short getComensalNumero() {
        return comensalNumero;
    }

    public OffsetDateTime getEnviadaEn() {
        return enviadaEn;
    }

    public OffsetDateTime getListaEn() {
        return listaEn;
    }

    public OffsetDateTime getEntregadaEn() {
        return entregadaEn;
    }
}
