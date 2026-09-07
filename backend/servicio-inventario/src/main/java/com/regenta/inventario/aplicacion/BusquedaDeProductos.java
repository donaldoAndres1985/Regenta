package com.regenta.inventario.aplicacion;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.comun.errores.NoEncontradoException;
import com.regenta.comun.errores.RecursoDuplicadoException;
import com.regenta.comun.errores.ReglaDeNegocioException;
import com.regenta.comun.negocio.ContextoDeNegocio;
import com.regenta.comun.negocio.RequierePermiso;
import com.regenta.inventario.domain.Categoria;
import com.regenta.inventario.domain.Existencia;
import com.regenta.inventario.domain.NivelStock;
import com.regenta.inventario.domain.Producto;
import com.regenta.inventario.domain.ProductoCodigo;
import com.regenta.inventario.infra.CategoriaRepositorio;
import com.regenta.inventario.infra.ExistenciaRepositorio;
import com.regenta.inventario.infra.ProductoCodigoRepositorio;
import com.regenta.inventario.infra.ProductoRepositorio;

/**
 * Búsqueda de productos y resolución de código de barras. HU-035.
 *
 * <p>El vendedor encuentra un producto por nombre, SKU o código (criterio 1), o
 * escaneándolo: un código propio resuelve con factor 1, uno alterno con su
 * factor de conversión (criterio 4). El nivel de stock (NORMAL / BAJO / CERO)
 * viaja en cada resultado para pintar la cantidad con su color.
 */
@Service
public class BusquedaDeProductos {

    private static final int LIMITE_POR_DEFECTO = 50;

    private final ProductoRepositorio productos;
    private final ProductoCodigoRepositorio codigos;
    private final CategoriaRepositorio categorias;
    private final ExistenciaRepositorio existencias;

    public BusquedaDeProductos(ProductoRepositorio productos, ProductoCodigoRepositorio codigos,
            CategoriaRepositorio categorias, ExistenciaRepositorio existencias) {
        this.productos = productos;
        this.codigos = codigos;
        this.categorias = categorias;
        this.existencias = existencias;
    }

    @Transactional(readOnly = true)
    @RequierePermiso("INVENTARIO_PRODUCTO_VER")
    public ResultadoDeBusqueda buscar(FiltroDeBusqueda filtro) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        String q = filtro.q() == null ? "" : filtro.q().trim();

        List<Producto> encontrados;
        if (q.isBlank()) {
            encontrados = productos.findByNegocioIdAndActivoTrueOrderByNombreAsc(negocioId);
        } else {
            if (q.length() < 3) {
                throw new ReglaDeNegocioException(
                        "Escribe al menos tres caracteres para buscar");
            }
            int limite = filtro.limite() != null && filtro.limite() > 0
                    ? filtro.limite() : LIMITE_POR_DEFECTO;
            encontrados = productos.buscar(negocioId, "%" + q.toLowerCase() + "%",
                    PageRequest.of(0, limite));
        }

        Map<UUID, String> nombreDeCategoria = categorias
                .findByNegocioIdOrderByNivelAscOrdenAscNombreAsc(negocioId).stream()
                .collect(Collectors.toMap(Categoria::getId, Categoria::getNombre));

        List<ProductoEncontrado> filas = new ArrayList<>();
        for (Producto producto : encontrados) {
            if (filtro.categoriaId() != null
                    && !filtro.categoriaId().equals(producto.getCategoriaId())) {
                continue;
            }
            BigDecimal stock = existencias.findByProductoIdOrderByBodegaId(producto.getId()).stream()
                    .map(Existencia::getCantidad).reduce(BigDecimal.ZERO, BigDecimal::add);
            NivelStock nivel = nivelDe(stock, producto.getStockMinimo());
            if (filtro.soloBajoMinimo() && nivel == NivelStock.NORMAL) {
                continue;
            }
            filas.add(new ProductoEncontrado(producto.getId(), producto.getSku(),
                    producto.getCodigoBarras(), producto.getNombre(), producto.getCategoriaId(),
                    nombreDeCategoria.getOrDefault(producto.getCategoriaId(), "?"),
                    producto.getPrecioVenta(), stock, nivel));
        }
        return new ResultadoDeBusqueda(filas, filas.size());
    }

    @Transactional(readOnly = true)
    @RequierePermiso("INVENTARIO_PRODUCTO_VER")
    public CodigoResuelto resolverCodigo(String codigo) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        String c = codigo == null ? "" : codigo.trim();

        Producto propio = productos.findByNegocioIdAndCodigoBarras(negocioId, c).orElse(null);
        if (propio != null) {
            return new CodigoResuelto(propio.getId(), propio.getSku(), propio.getNombre(),
                    BigDecimal.ONE, false);
        }

        ProductoCodigo alterno = codigos.findByNegocioIdAndCodigo(negocioId, c)
                .orElseThrow(() -> new NoEncontradoException(
                        "Ningun producto tiene el codigo " + c));
        Producto producto = productos.findById(alterno.getProductoId())
                .orElseThrow(() -> new NoEncontradoException("Ese producto no existe"));
        return new CodigoResuelto(producto.getId(), producto.getSku(), producto.getNombre(),
                alterno.getFactor(), true);
    }

    @Transactional
    @RequierePermiso("INVENTARIO_PRODUCTO_CREAR")
    public void agregarCodigo(UUID productoId, SolicitudDeCodigo solicitud) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        productoDelNegocio(productoId);
        String c = solicitud.codigo().trim();
        if (codigos.existsByNegocioIdAndCodigo(negocioId, c)
                || productos.existsByNegocioIdAndCodigoBarras(negocioId, c)) {
            throw new RecursoDuplicadoException("Ya hay un producto con el codigo " + c);
        }
        codigos.save(ProductoCodigo.de(negocioId, productoId, c, solicitud.factor(),
                solicitud.descripcion()));
    }

    private static NivelStock nivelDe(BigDecimal stock, BigDecimal minimo) {
        if (stock.signum() <= 0) {
            return NivelStock.CERO;
        }
        if (minimo != null && minimo.signum() > 0 && stock.compareTo(minimo) <= 0) {
            return NivelStock.BAJO;
        }
        return NivelStock.NORMAL;
    }

    private Producto productoDelNegocio(UUID productoId) {
        Producto producto = productos.findById(productoId)
                .orElseThrow(() -> new NoEncontradoException("Ese producto no existe"));
        if (!producto.getNegocioId().equals(ContextoDeNegocio.negocioActual())) {
            throw new NoEncontradoException("Ese producto no existe");
        }
        return producto;
    }
}
