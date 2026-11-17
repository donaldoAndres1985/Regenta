package com.regenta.inventario.domain;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

import com.regenta.shared.web.ReglaDeNegocioException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

/** HU-157 · el precio de una fracción para un solo producto; le gana al de la tabla (I14). */
@Entity
@Table(name = "precios_propios_fraccion", schema = "inventario")
@IdClass(PrecioPropioDeFraccion.Clave.class)
public class PrecioPropioDeFraccion {

    @Id
    @Column(name = "producto_id")
    private UUID productoId;

    @Id
    @Column(length = 30)
    private String medida;

    @Column(name = "negocio_id", nullable = false, updatable = false)
    private UUID negocioId;

    @Column(nullable = false)
    private BigDecimal precio;

    protected PrecioPropioDeFraccion() {
    }

    public static PrecioPropioDeFraccion de(UUID negocioId, UUID productoId, String medida, BigDecimal precio) {
        PrecioPropioDeFraccion p = new PrecioPropioDeFraccion();
        p.negocioId = negocioId;
        p.productoId = productoId;
        p.medida = medida;
        p.cambiarPrecio(precio);
        return p;
    }

    public void cambiarPrecio(BigDecimal precio) {
        if (precio == null || precio.signum() <= 0) {
            throw new ReglaDeNegocioException("Escribe el precio de «" + medida + "».");
        }
        this.precio = precio;
    }

    public UUID getProductoId() {
        return productoId;
    }

    public String getMedida() {
        return medida;
    }

    public BigDecimal getPrecio() {
        return precio;
    }

    /** Clave compuesta: producto y medida. */
    public static class Clave implements Serializable {
        private UUID productoId;
        private String medida;

        public Clave() {
        }

        public Clave(UUID productoId, String medida) {
            this.productoId = productoId;
            this.medida = medida;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Clave c && Objects.equals(productoId, c.productoId)
                    && Objects.equals(medida, c.medida);
        }

        @Override
        public int hashCode() {
            return Objects.hash(productoId, medida);
        }
    }
}
