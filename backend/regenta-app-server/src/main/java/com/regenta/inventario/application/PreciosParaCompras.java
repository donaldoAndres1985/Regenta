package com.regenta.inventario.application;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.inventario.api.CostoYPrecio;
import com.regenta.inventario.domain.Existencia;
import com.regenta.inventario.domain.PrecioConIva;
import com.regenta.inventario.domain.Producto;
import com.regenta.inventario.infrastructure.ExistenciaRepositorio;
import com.regenta.inventario.infrastructure.ProductoRepositorio;
import com.regenta.shared.tenancy.ContextoDeNegocio;
import com.regenta.shared.web.NoEncontradoException;
import com.regenta.shared.web.ReglaDeNegocioException;

/**
 * HU-179 · El costo y el precio de los productos que llegan en una recepción, y el precio nuevo que
 * se decide al recibirlos. El permiso ({@code INVENTARIO_PRODUCTO_EDITAR}) lo exige Compras antes de
 * llamar: aquí solo se valida el precio.
 */
@Service
public class PreciosParaCompras {

    private final ProductoRepositorio productos;
    private final ExistenciaRepositorio existencias;

    public PreciosParaCompras(ProductoRepositorio productos, ExistenciaRepositorio existencias) {
        this.productos = productos;
        this.existencias = existencias;
    }

    /** Sin existencia en la bodega, el costo promedio es el último costo del producto. */
    @Transactional(readOnly = true)
    public Map<UUID, CostoYPrecio> costosYPrecios(UUID negocioId, UUID bodegaId, Collection<UUID> productoIds) {
        Map<UUID, CostoYPrecio> respuesta = new LinkedHashMap<>();
        for (Producto p : productos.findAllById(productoIds)) {
            if (!negocioId.equals(p.getNegocioId())) {
                continue;
            }
            BigDecimal promedio = existencias.findByProductoIdAndBodegaId(p.getId(), bodegaId)
                    .map(Existencia::getCostoPromedio)
                    .filter(c -> c != null && c.signum() > 0)
                    .orElse(p.getCostoUltimo());
            PrecioConIva precio = p.precio();
            respuesta.put(p.getId(), new CostoYPrecio(p.getId(), promedio, precio.precio(), precio.base(),
                    p.getTarifaIva().name()));
        }
        return respuesta;
    }

    /** C2 · El precio de venta nuevo, con IVA incluido (I2). */
    @Transactional
    public void cambiarPrecioDeVenta(UUID negocioId, UUID productoId, BigDecimal precioConIva) {
        if (precioConIva == null || precioConIva.signum() <= 0) {
            throw new ReglaDeNegocioException("El precio de venta nuevo debe ser mayor que cero");
        }
        Producto producto = productos.findById(productoId)
                .filter(p -> negocioId.equals(p.getNegocioId()))
                .orElseThrow(() -> new NoEncontradoException("Ese producto no existe"));
        producto.cambiarPrecioDeVenta(precioConIva);
        producto.tocar(ContextoDeNegocio.actual().usuario());
        productos.save(producto);
    }
}
