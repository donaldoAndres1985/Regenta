package com.regenta.inventario.domain;

import java.math.BigDecimal;
import java.util.UUID;

import com.regenta.comun.errores.ReglaDeNegocioException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Una linea de un traslado: un producto y cuanto se movio. HU-032. */
@Entity
@Table(name = "traslado_lineas")
public class TrasladoLinea {

    @Id
    private UUID id;

    @Column(name = "negocio_id", nullable = false, updatable = false)
    private UUID negocioId;

    @Column(name = "traslado_id", nullable = false, updatable = false)
    private UUID trasladoId;

    @Column(name = "producto_id", nullable = false, updatable = false)
    private UUID productoId;

    @Column(name = "lote_id", updatable = false)
    private UUID loteId;

    @Column(name = "cantidad_enviada", nullable = false, updatable = false)
    private BigDecimal cantidadEnviada;

    @Column(name = "cantidad_recibida", nullable = false)
    private BigDecimal cantidadRecibida;

    protected TrasladoLinea() {
    }

    public static TrasladoLinea de(UUID negocioId, UUID trasladoId, UUID productoId, UUID loteId,
            BigDecimal cantidadEnviada) {
        TrasladoLinea l = new TrasladoLinea();
        l.id = UUID.randomUUID();
        l.negocioId = negocioId;
        l.trasladoId = trasladoId;
        l.productoId = productoId;
        l.loteId = loteId;
        l.cantidadEnviada = cantidadEnviada;
        l.cantidadRecibida = BigDecimal.ZERO;
        return l;
    }

    /** Suma lo recibido en esta entrega. No puede pasar de lo enviado (criterio 3). */
    public void registrarRecepcion(BigDecimal cantidad) {
        BigDecimal total = this.cantidadRecibida.add(cantidad);
        if (total.compareTo(cantidadEnviada) > 0) {
            throw new ReglaDeNegocioException("No se puede recibir mas de lo enviado: se enviaron "
                    + cantidadEnviada + " y ya van " + total);
        }
        this.cantidadRecibida = total;
    }

    /** Lo que falta por recibir de esta linea. */
    public BigDecimal pendiente() {
        return cantidadEnviada.subtract(cantidadRecibida);
    }

    public UUID getId() {
        return id;
    }

    public UUID getTrasladoId() {
        return trasladoId;
    }

    public UUID getProductoId() {
        return productoId;
    }

    public UUID getLoteId() {
        return loteId;
    }

    public BigDecimal getCantidadEnviada() {
        return cantidadEnviada;
    }

    public BigDecimal getCantidadRecibida() {
        return cantidadRecibida;
    }
}
