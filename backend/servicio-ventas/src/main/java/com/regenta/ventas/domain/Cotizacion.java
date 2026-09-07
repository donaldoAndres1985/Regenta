package com.regenta.ventas.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import com.regenta.comun.errores.ConflictoDeEstadoException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Una cotización. HU-044.
 *
 * <p>Se arma con sus líneas (snapshot del producto, como en la venta) y luego
 * se convierte en una venta en borrador. Al convertirla queda enlazada a la
 * venta resultante y no se vuelve a convertir. Si ya venció, la conversión
 * avisa que los precios pueden haber cambiado, pero no se bloquea.
 */
@Entity
@Table(name = "cotizaciones")
public class Cotizacion {

    @Id
    private UUID id;

    @Column(name = "negocio_id", nullable = false, updatable = false)
    private UUID negocioId;

    @Column(nullable = false, length = 30, updatable = false)
    private String numero;

    @Column(name = "cliente_id")
    private UUID clienteId;

    @Column(name = "usuario_id", updatable = false)
    private UUID usuarioId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoCotizacion estado;

    @Column(name = "valida_hasta")
    private LocalDate validaHasta;

    @Column(nullable = false)
    private BigDecimal total;

    @Column(name = "venta_id")
    private UUID ventaId;

    @CreationTimestamp
    @Column(name = "creado_en", nullable = false, updatable = false)
    private OffsetDateTime creadoEn;

    protected Cotizacion() {
    }

    public static Cotizacion crear(UUID negocioId, String numero, UUID clienteId, UUID usuarioId,
            LocalDate validaHasta) {
        Cotizacion c = new Cotizacion();
        c.id = UUID.randomUUID();
        c.negocioId = negocioId;
        c.numero = numero;
        c.clienteId = clienteId;
        c.usuarioId = usuarioId;
        c.validaHasta = validaHasta;
        c.estado = EstadoCotizacion.ABIERTA;
        c.total = BigDecimal.ZERO;
        return c;
    }

    public void fijarTotal(BigDecimal total) {
        this.total = total;
    }

    /** Vencida = ya pasó su fecha de validez. */
    public boolean estaVencida(LocalDate hoy) {
        return validaHasta != null && hoy.isAfter(validaHasta);
    }

    /** Enlaza la cotización a la venta creada y la cierra (criterios 1 y 3). */
    public void marcarConvertida(UUID ventaId) {
        if (estado == EstadoCotizacion.CONVERTIDA) {
            throw new ConflictoDeEstadoException("La cotizacion " + numero
                    + " ya se convirtio en la venta " + this.ventaId);
        }
        if (estado == EstadoCotizacion.RECHAZADA) {
            throw new ConflictoDeEstadoException(
                    "La cotizacion " + numero + " esta RECHAZADA y no se convierte");
        }
        this.estado = EstadoCotizacion.CONVERTIDA;
        this.ventaId = ventaId;
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

    public UUID getClienteId() {
        return clienteId;
    }

    public EstadoCotizacion getEstado() {
        return estado;
    }

    public LocalDate getValidaHasta() {
        return validaHasta;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public UUID getVentaId() {
        return ventaId;
    }
}
