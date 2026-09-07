package com.regenta.inventario.domain;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/** PK compuesta de {@code precios_producto}: {@code (lista_id, producto_id, cantidad_minima)}. */
public class PrecioDeProductoId implements Serializable {

    private static final long serialVersionUID = 1L;

    private UUID listaId;
    private UUID productoId;
    private BigDecimal cantidadMinima;

    public PrecioDeProductoId() {
    }

    public PrecioDeProductoId(UUID listaId, UUID productoId, BigDecimal cantidadMinima) {
        this.listaId = listaId;
        this.productoId = productoId;
        this.cantidadMinima = cantidadMinima;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PrecioDeProductoId otro)) {
            return false;
        }
        return Objects.equals(listaId, otro.listaId)
                && Objects.equals(productoId, otro.productoId)
                && Objects.equals(cantidadMinima, otro.cantidadMinima);
    }

    @Override
    public int hashCode() {
        return Objects.hash(listaId, productoId, cantidadMinima);
    }
}
