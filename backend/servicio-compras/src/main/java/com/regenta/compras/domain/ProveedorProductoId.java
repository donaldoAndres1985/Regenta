package com.regenta.compras.domain;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/** Clave compuesta de {@link ProveedorProducto}: {@code (proveedor_id, producto_id)}. */
public class ProveedorProductoId implements Serializable {

    private static final long serialVersionUID = 1L;

    private UUID proveedorId;
    private UUID productoId;

    public ProveedorProductoId() {
    }

    public ProveedorProductoId(UUID proveedorId, UUID productoId) {
        this.proveedorId = proveedorId;
        this.productoId = productoId;
    }

    public UUID getProveedorId() {
        return proveedorId;
    }

    public UUID getProductoId() {
        return productoId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ProveedorProductoId otra)) {
            return false;
        }
        return Objects.equals(proveedorId, otra.proveedorId)
                && Objects.equals(productoId, otra.productoId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(proveedorId, productoId);
    }
}
