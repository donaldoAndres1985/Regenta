package com.regenta.inventario.application;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.inventario.domain.Existencia;
import com.regenta.inventario.infrastructure.BodegaRepositorio;
import com.regenta.inventario.infrastructure.ExistenciaRepositorio;
import com.regenta.inventario.infrastructure.ProductoRepositorio;
import com.regenta.shared.security.RequierePermiso;
import com.regenta.shared.tenancy.ContextoDeNegocio;
import com.regenta.shared.web.NoEncontradoException;

/**
 * HU-152 · la ubicación de un producto dentro de una bodega (I9). Si el producto todavía no tiene
 * fila en esa bodega, se crea en cero: decir dónde va no mueve existencias.
 */
@Service
public class GestionDeUbicaciones {

    private final ExistenciaRepositorio existencias;
    private final ProductoRepositorio productos;
    private final BodegaRepositorio bodegas;

    public GestionDeUbicaciones(ExistenciaRepositorio existencias, ProductoRepositorio productos,
            BodegaRepositorio bodegas) {
        this.existencias = existencias;
        this.productos = productos;
        this.bodegas = bodegas;
    }

    @Transactional
    @RequierePermiso("INVENTARIO_PRODUCTO_EDITAR")
    public void fijar(UUID productoId, UUID bodegaId, String ubicacion) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        productos.findById(productoId).filter(p -> p.getNegocioId().equals(negocioId))
                .orElseThrow(() -> new NoEncontradoException("Ese producto no existe"));
        bodegas.findById(bodegaId).filter(b -> b.getNegocioId().equals(negocioId))
                .orElseThrow(() -> new NoEncontradoException("Esa bodega no existe"));
        Existencia existencia = existencias.findByProductoIdAndBodegaId(productoId, bodegaId)
                .orElseGet(() -> Existencia.enCero(negocioId, productoId, bodegaId));
        existencia.fijarUbicacion(ubicacion);
        existencias.save(existencia);
    }
}
