package com.regenta.inventario.aplicacion;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.comun.errores.NoEncontradoException;
import com.regenta.comun.errores.RecursoDuplicadoException;
import com.regenta.comun.eventos.RegistroDeEventos;
import com.regenta.comun.negocio.ContextoDeNegocio;
import com.regenta.comun.negocio.RequierePermiso;
import com.regenta.inventario.domain.Categoria;
import com.regenta.inventario.domain.Producto;
import com.regenta.inventario.domain.UnidadMedida;
import com.regenta.inventario.infra.AtributoCategoriaRepositorio;
import com.regenta.inventario.infra.CategoriaRepositorio;
import com.regenta.inventario.infra.ProductoRepositorio;
import com.regenta.inventario.infra.UnidadMedidaRepositorio;

/**
 * El catalogo. HU-028: crear un producto y que el sistema exija los campos de
 * su categoria, sin pedir un desarrollo por cada tipo de producto nuevo.
 *
 * <p>Los atributos variables van al JSONB {@code atributos}, validados contra
 * {@code atributos_categoria} por {@link ValidadorDeAtributos} antes de
 * persistir. Que un perecedero maneje lotes lo exige el CHECK de la base.
 */
@Service
public class GestionDeProductos {

    private final ProductoRepositorio productos;
    private final CategoriaRepositorio categorias;
    private final AtributoCategoriaRepositorio atributos;
    private final UnidadMedidaRepositorio unidades;
    private final ValidadorDeAtributos validador;
    private final RegistroDeEventos eventos;

    public GestionDeProductos(ProductoRepositorio productos, CategoriaRepositorio categorias,
            AtributoCategoriaRepositorio atributos, UnidadMedidaRepositorio unidades,
            ValidadorDeAtributos validador, RegistroDeEventos eventos) {
        this.productos = productos;
        this.categorias = categorias;
        this.atributos = atributos;
        this.unidades = unidades;
        this.validador = validador;
        this.eventos = eventos;
    }

    @Transactional
    @RequierePermiso("INVENTARIO_PRODUCTO_CREAR")
    public ProductoDelNegocio crear(SolicitudDeProducto solicitud) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        categoriaDelNegocio(solicitud.categoriaId());
        unidadDelNegocio(solicitud.unidadMedidaId());

        if (productos.existsByNegocioIdAndSku(negocioId, solicitud.sku())) {
            throw new RecursoDuplicadoException(
                    "Ya hay un producto con el SKU " + solicitud.sku());
        }
        String codigoBarras = normalizar(solicitud.codigoBarras());
        if (codigoBarras != null
                && productos.existsByNegocioIdAndCodigoBarras(negocioId, codigoBarras)) {
            throw new RecursoDuplicadoException(
                    "Ya hay un producto con el codigo de barras " + codigoBarras);
        }

        Map<String, Object> validados = validador.validar(
                atributos.findByCategoriaIdOrderByOrdenAscNombreCampoAsc(solicitud.categoriaId()),
                solicitud.atributos());

        Producto producto = Producto.nuevo(negocioId, solicitud.sku(), solicitud.nombre(),
                solicitud.categoriaId(), solicitud.unidadMedidaId(),
                ContextoDeNegocio.actual().usuario());
        producto.datosBasicos(solicitud.descripcion(), solicitud.marcaId(), solicitud.tipo(),
                solicitud.precioVenta(), codigoBarras);
        producto.configuracionDeStock(solicitud.controlaStock(), solicitud.stockMinimo(),
                solicitud.stockMaximo(), solicitud.manejaLotes(), solicitud.manejaSeries(),
                solicitud.perecedero(), solicitud.permiteVentaSinStock());
        producto.fijarAtributos(validados);

        // El CHECK ck_lotes_perecedero de la base es la ultima palabra: un
        // perecedero sin maneja_lotes no entra.
        productos.saveAndFlush(producto);

        eventos.registrar(negocioId, "producto", producto.getId(), "producto_creado",
                datos(negocioId, producto));
        return comoDto(producto);
    }

    @Transactional(readOnly = true)
    @RequierePermiso("INVENTARIO_PRODUCTO_VER")
    public ProductoDelNegocio ver(UUID productoId) {
        Producto producto = productos.findById(productoId)
                .orElseThrow(() -> new NoEncontradoException("Ese producto no existe"));
        if (!producto.getNegocioId().equals(ContextoDeNegocio.negocioActual())) {
            throw new NoEncontradoException("Ese producto no existe");
        }
        return comoDto(producto);
    }

    private Categoria categoriaDelNegocio(UUID categoriaId) {
        Categoria categoria = categorias.findById(categoriaId)
                .orElseThrow(() -> new NoEncontradoException("Esa categoria no existe"));
        if (!categoria.getNegocioId().equals(ContextoDeNegocio.negocioActual())) {
            throw new NoEncontradoException("Esa categoria no existe");
        }
        return categoria;
    }

    private UnidadMedida unidadDelNegocio(UUID unidadId) {
        UnidadMedida unidad = unidades.findById(unidadId)
                .orElseThrow(() -> new NoEncontradoException("Esa unidad de medida no existe"));
        if (!unidad.getNegocioId().equals(ContextoDeNegocio.negocioActual())) {
            throw new NoEncontradoException("Esa unidad de medida no existe");
        }
        return unidad;
    }

    private static String normalizar(String codigoBarras) {
        return (codigoBarras == null || codigoBarras.isBlank()) ? null : codigoBarras.trim();
    }

    private static Map<String, Object> datos(UUID negocioId, Producto producto) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("negocio_id", negocioId.toString());
        payload.put("producto_id", producto.getId().toString());
        payload.put("sku", producto.getSku());
        payload.put("nombre", producto.getNombre());
        payload.put("categoria_id", producto.getCategoriaId().toString());
        return payload;
    }

    private static ProductoDelNegocio comoDto(Producto p) {
        return new ProductoDelNegocio(p.getId(), p.getSku(), p.getCodigoBarras(), p.getNombre(),
                p.getCategoriaId(), p.getUnidadMedidaId(), p.getTipo(), p.getPrecioVenta(),
                p.isManejaLotes(), p.isPerecedero(), p.getAtributos(), p.isActivo());
    }

    /** Solo para tests y para el alta inicial del negocio: crea una unidad. */
    @Transactional
    @RequierePermiso("INVENTARIO_PRODUCTO_CREAR")
    public UUID crearUnidad(String codigo, String nombre, boolean permiteDecimales) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        return unidades.findByNegocioIdAndCodigoIgnoreCase(negocioId, codigo)
                .map(UnidadMedida::getId)
                .orElseGet(() -> unidades
                        .save(UnidadMedida.nueva(negocioId, codigo, nombre, permiteDecimales))
                        .getId());
    }

    @Transactional(readOnly = true)
    @RequierePermiso("INVENTARIO_PRODUCTO_VER")
    public List<UnidadMedida> unidades() {
        return unidades.findByNegocioIdOrderByCodigo(ContextoDeNegocio.negocioActual());
    }
}
