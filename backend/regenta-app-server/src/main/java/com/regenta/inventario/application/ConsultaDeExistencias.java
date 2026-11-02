package com.regenta.inventario.application;

import com.regenta.inventario.api.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.shared.web.NoEncontradoException;
import com.regenta.shared.tenancy.ContextoDeNegocio;
import com.regenta.shared.security.RequierePermiso;
import com.regenta.inventario.domain.Bodega;
import com.regenta.inventario.domain.Existencia;
import com.regenta.inventario.infrastructure.BodegaRepositorio;
import com.regenta.inventario.infrastructure.ExistenciaRepositorio;
import com.regenta.inventario.infrastructure.ProductoRepositorio;

/** El stock de un producto: cuanto hay en cada bodega y el total. HU-029. */
@Service
public class ConsultaDeExistencias {

    private final ExistenciaRepositorio existencias;
    private final BodegaRepositorio bodegas;
    private final ProductoRepositorio productos;

    public ConsultaDeExistencias(ExistenciaRepositorio existencias, BodegaRepositorio bodegas,
            ProductoRepositorio productos) {
        this.existencias = existencias;
        this.bodegas = bodegas;
        this.productos = productos;
    }

    @Transactional(readOnly = true)
    @RequierePermiso("INVENTARIO_EXISTENCIA_VER")
    public StockDeProducto delProducto(UUID productoId) {
        productos.findById(productoId).filter(p -> p.getNegocioId()
                .equals(ContextoDeNegocio.negocioActual()))
                .orElseThrow(() -> new NoEncontradoException("Ese producto no existe"));

        Map<UUID, String> nombreDeBodega = bodegas
                .findByNegocioIdOrderByCodigo(ContextoDeNegocio.negocioActual()).stream()
                .collect(Collectors.toMap(Bodega::getId, Bodega::getNombre));

        List<ExistenciaEnBodega> lineas = existencias.findByProductoIdOrderByBodegaId(productoId)
                .stream()
                .map(e -> new ExistenciaEnBodega(e.getBodegaId(),
                        nombreDeBodega.getOrDefault(e.getBodegaId(), "?"),
                        e.getCantidad(), e.getCantidadReservada(), e.getCantidadDisponible()))
                .toList();

        BigDecimal total = lineas.stream().map(ExistenciaEnBodega::cantidad)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal disponible = lineas.stream().map(ExistenciaEnBodega::cantidadDisponible)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new StockDeProducto(productoId, lineas, total, disponible);
    }

    @Transactional(readOnly = true)
    @RequierePermiso("INVENTARIO_EXISTENCIA_VER")
    public List<Existencia> deLaBodega(UUID bodegaId) {
        return existencias.findByBodegaId(bodegaId);
    }
}
