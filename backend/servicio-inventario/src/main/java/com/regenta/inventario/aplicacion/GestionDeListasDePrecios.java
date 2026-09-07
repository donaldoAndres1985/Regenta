package com.regenta.inventario.aplicacion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.comun.errores.NoEncontradoException;
import com.regenta.comun.errores.RecursoDuplicadoException;
import com.regenta.comun.errores.ReglaDeNegocioException;
import com.regenta.comun.negocio.ContextoDeNegocio;
import com.regenta.comun.negocio.RequierePermiso;
import com.regenta.inventario.domain.ListaDePrecios;
import com.regenta.inventario.domain.PrecioDeProducto;
import com.regenta.inventario.domain.PrecioDeProductoId;
import com.regenta.inventario.domain.Producto;
import com.regenta.inventario.infra.ListaDePreciosRepositorio;
import com.regenta.inventario.infra.PrecioDeProductoRepositorio;
import com.regenta.inventario.infra.ProductoRepositorio;

/**
 * Listas de precios y precios por volumen. HU-036.
 *
 * <p>Una lista guarda un precio distinto para cada producto —así se le vende al
 * mayorista sin duplicar el catálogo (criterio 1)— y varios tramos por cantidad
 * (criterio 2). Una lista con vigencia vencida no aparece ni se puede usar
 * (criterio 3), y un descuento sobre el máximo de la lista se rechaza
 * (criterio 4).
 */
@Service
public class GestionDeListasDePrecios {

    private final ListaDePreciosRepositorio listas;
    private final PrecioDeProductoRepositorio precios;
    private final ProductoRepositorio productos;

    public GestionDeListasDePrecios(ListaDePreciosRepositorio listas,
            PrecioDeProductoRepositorio precios, ProductoRepositorio productos) {
        this.listas = listas;
        this.precios = precios;
        this.productos = productos;
    }

    @Transactional
    @RequierePermiso("INVENTARIO_LISTA_PRECIOS_GESTIONAR")
    public ListaDelNegocio crearLista(SolicitudDeLista solicitud) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        if (listas.existsByNegocioIdAndNombreIgnoreCase(negocioId, solicitud.nombre())) {
            throw new RecursoDuplicadoException(
                    "Ya hay una lista de precios con el nombre " + solicitud.nombre());
        }
        ListaDePrecios lista = ListaDePrecios.nueva(negocioId, solicitud.nombre(),
                solicitud.moneda(), solicitud.esDefault(), solicitud.vigenteDesde(),
                solicitud.vigenteHasta());
        listas.save(lista);
        return comoDto(lista);
    }

    @Transactional
    @RequierePermiso("INVENTARIO_LISTA_PRECIOS_GESTIONAR")
    public void fijarPrecio(SolicitudDePrecio solicitud) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        listaDelNegocio(solicitud.listaId());
        productoDelNegocio(solicitud.productoId());

        PrecioDeProductoId id = new PrecioDeProductoId(solicitud.listaId(), solicitud.productoId(),
                solicitud.cantidadMinima());
        PrecioDeProducto precio = precios.findById(id).orElse(null);
        if (precio == null) {
            precio = PrecioDeProducto.de(negocioId, solicitud.listaId(), solicitud.productoId(),
                    solicitud.cantidadMinima(), solicitud.precio(), solicitud.descuentoMaxPct());
        } else {
            precio.ajustar(solicitud.precio(), solicitud.descuentoMaxPct());
        }
        precios.save(precio);
    }

    /** Criterio 3: solo las listas vigentes. */
    @Transactional(readOnly = true)
    @RequierePermiso("INVENTARIO_LISTA_PRECIOS_VER")
    public List<ListaDelNegocio> listasVigentes() {
        LocalDate hoy = LocalDate.now();
        return listas.findByNegocioIdOrderByNombre(ContextoDeNegocio.negocioActual()).stream()
                .filter(l -> l.vigente(hoy))
                .map(GestionDeListasDePrecios::comoDto)
                .toList();
    }

    /** Criterios 1 y 2: el precio que aplica para un producto en una lista, dada la cantidad. */
    @Transactional(readOnly = true)
    @RequierePermiso("INVENTARIO_LISTA_PRECIOS_VER")
    public PrecioResuelto resolverPrecio(UUID listaId, UUID productoId, BigDecimal cantidad) {
        ListaDePrecios lista = listaDelNegocio(listaId);
        if (!lista.vigente(LocalDate.now())) {
            throw new ReglaDeNegocioException(
                    "La lista \"" + lista.getNombre() + "\" no esta vigente");
        }
        List<PrecioDeProducto> tramos = precios
                .findByListaIdAndProductoIdOrderByCantidadMinimaAsc(listaId, productoId);
        if (tramos.isEmpty()) {
            throw new NoEncontradoException("El producto no tiene precio en esa lista");
        }
        PrecioDeProducto aplica = tramos.get(0);
        for (PrecioDeProducto tramo : tramos) {
            if (tramo.getCantidadMinima().compareTo(cantidad) <= 0) {
                aplica = tramo;
            }
        }
        return new PrecioResuelto(productoId, listaId, aplica.getPrecio(),
                aplica.getCantidadMinima(), aplica.getDescuentoMaxPct());
    }

    /** Criterio 4: un descuento sobre el máximo de la lista se rechaza. */
    @Transactional(readOnly = true)
    @RequierePermiso("INVENTARIO_LISTA_PRECIOS_VER")
    public PrecioResuelto validarDescuento(UUID listaId, UUID productoId, BigDecimal cantidad,
            BigDecimal descuentoPct) {
        PrecioResuelto resuelto = resolverPrecio(listaId, productoId, cantidad);
        if (descuentoPct != null && descuentoPct.compareTo(resuelto.descuentoMaxPct()) > 0) {
            throw new ReglaDeNegocioException("El descuento " + descuentoPct
                    + "% supera el maximo permitido de la lista (" + resuelto.descuentoMaxPct()
                    + "%)");
        }
        return resuelto;
    }

    private ListaDePrecios listaDelNegocio(UUID listaId) {
        ListaDePrecios lista = listas.findById(listaId)
                .orElseThrow(() -> new NoEncontradoException("Esa lista de precios no existe"));
        if (!lista.getNegocioId().equals(ContextoDeNegocio.negocioActual())) {
            throw new NoEncontradoException("Esa lista de precios no existe");
        }
        return lista;
    }

    private Producto productoDelNegocio(UUID productoId) {
        Producto producto = productos.findById(productoId)
                .orElseThrow(() -> new NoEncontradoException("Ese producto no existe"));
        if (!producto.getNegocioId().equals(ContextoDeNegocio.negocioActual())) {
            throw new NoEncontradoException("Ese producto no existe");
        }
        return producto;
    }

    private static ListaDelNegocio comoDto(ListaDePrecios l) {
        return new ListaDelNegocio(l.getId(), l.getNombre(), l.getMoneda(), l.isEsDefault(),
                l.getVigenteDesde(), l.getVigenteHasta(), l.isActiva());
    }
}
