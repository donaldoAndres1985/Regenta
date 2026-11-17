package com.regenta.inventario.application;

import com.regenta.inventario.api.*;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.shared.web.ConflictoDeEstadoException;
import com.regenta.shared.web.NoEncontradoException;
import com.regenta.shared.web.RecursoDuplicadoException;
import com.regenta.shared.events.RegistroDeEventos;
import com.regenta.shared.tenancy.ContextoDeNegocio;
import com.regenta.shared.security.RequierePermiso;
import com.regenta.inventario.domain.Categoria;
import com.regenta.inventario.domain.Producto;
import com.regenta.inventario.domain.UnidadMedida;
import com.regenta.inventario.infrastructure.AtributoCategoriaRepositorio;
import com.regenta.inventario.infrastructure.CategoriaRepositorio;
import com.regenta.inventario.infrastructure.ProductoRepositorio;
import com.regenta.inventario.infrastructure.UnidadMedidaRepositorio;

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
                solicitud.categoriaId(), solicitud.unidadMedidaId(), solicitud.tarifaIva(),
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

    /**
     * HU-148 · para Ventas (por {@code InventarioApi}): sin permiso propio, porque quien vende no
     * necesita el de ver el catálogo; sí exige que el producto sea del negocio y esté activo (I8).
     */
    @Transactional(readOnly = true)
    public java.util.Optional<PrecioDeCatalogo> precioDeCatalogo(UUID negocioId, UUID productoId) {
        return productos.findById(productoId)
                .filter(p -> p.getNegocioId().equals(negocioId) && p.isActivo())
                .map(p -> new PrecioDeCatalogo(p.getPrecioVenta(), p.getTarifaIva().name(),
                        p.getTarifaIva().porcentaje()));
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

    static ProductoDelNegocio comoDto(Producto p) {
        BigDecimal base = p.precio().base();
        BigDecimal costo = p.getCostoUltimo();
        BigDecimal margen = null;
        BigDecimal beneficio = null;
        if (costo != null && costo.signum() > 0) {
            BigDecimal ganancia = base.subtract(costo);
            margen = ganancia.multiply(CIEN).divide(costo, 2, java.math.RoundingMode.HALF_UP);
            beneficio = base.signum() == 0 ? null
                    : ganancia.multiply(CIEN).divide(base, 2, java.math.RoundingMode.HALF_UP);
        }
        return new ProductoDelNegocio(p.getId(), p.getSku(), p.getCodigoBarras(), p.getNombre(),
                p.getCategoriaId(), p.getUnidadMedidaId(), p.getTipo(), p.getPrecioVenta(),
                p.getTarifaIva(), p.isTarifaPorRevisar(), base, p.precio().iva(), costo, margen,
                beneficio, p.isManejaLotes(), p.isPerecedero(), p.getAtributos(), p.isActivo(),
                p.getVersion());
    }

    private static final BigDecimal CIEN = new BigDecimal("100");

    /** HU-151 · deja de venderse; conserva su historial (I8). La confirmación la pide la app. */
    @Transactional
    @RequierePermiso("INVENTARIO_PRODUCTO_EDITAR")
    public ProductoDelNegocio desactivar(UUID productoId) {
        Producto producto = delNegocio(productoId);
        producto.desactivar();
        producto.tocar(ContextoDeNegocio.actual().usuario());
        return comoDto(productos.saveAndFlush(producto));
    }

    /** HU-151 C4. */
    @Transactional
    @RequierePermiso("INVENTARIO_PRODUCTO_EDITAR")
    public ProductoDelNegocio reactivar(UUID productoId) {
        Producto producto = delNegocio(productoId);
        producto.reactivar();
        producto.tocar(ContextoDeNegocio.actual().usuario());
        return comoDto(productos.saveAndFlush(producto));
    }

    private Producto delNegocio(UUID productoId) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        return productos.findById(productoId)
                .filter(p -> p.getNegocioId().equals(negocioId))
                .orElseThrow(() -> new NoEncontradoException("Ese producto no existe"));
    }

    /** HU-150 · guarda la ficha (C1-C5 de la historia). */
    @Transactional
    @RequierePermiso("INVENTARIO_PRODUCTO_EDITAR")
    public ProductoDelNegocio editar(UUID productoId, SolicitudDeEdicion solicitud) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        Producto producto = productos.findById(productoId)
                .filter(p -> p.getNegocioId().equals(negocioId))
                .orElseThrow(() -> new NoEncontradoException("Ese producto no existe"));
        if (producto.getVersion() != solicitud.version()) {
            throw new ConflictoDeEstadoException(
                    "Alguien cambió este producto; recarga antes de guardar.");
        }
        categoriaDelNegocio(solicitud.categoriaId());
        unidadDelNegocio(solicitud.unidadMedidaId());
        if (productos.existsByNegocioIdAndSkuAndIdNot(negocioId, solicitud.sku(), productoId)) {
            throw new RecursoDuplicadoException("Ese SKU ya lo usa otro producto.");
        }
        String codigoBarras = normalizar(solicitud.codigoBarras());
        if (codigoBarras != null && productos.existsByNegocioIdAndCodigoBarrasAndIdNot(negocioId,
                codigoBarras, productoId)) {
            throw new RecursoDuplicadoException("Ese código de barras ya lo usa otro producto.");
        }
        producto.editar(solicitud.sku(), codigoBarras, solicitud.nombre(), solicitud.categoriaId(),
                solicitud.unidadMedidaId(), solicitud.tarifaIva(), solicitud.precioVenta(),
                solicitud.costo());
        producto.tocar(ContextoDeNegocio.actual().usuario());
        return comoDto(productos.saveAndFlush(producto));
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

    /**
     * El ultimo costo de entrada de cada producto (HU-138, criterio 6). Un
     * producto sin costo conocido no aparece. La RLS acota al negocio.
     */
    @Transactional(readOnly = true)
    @RequierePermiso("INVENTARIO_PRODUCTO_VER")
    public Map<UUID, BigDecimal> costos(java.util.Collection<UUID> productoIds) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        Map<UUID, BigDecimal> costos = new LinkedHashMap<>();
        for (Producto p : productos.findAllById(productoIds)) {
            if (negocioId.equals(p.getNegocioId()) && p.getCostoUltimo().signum() > 0) {
                costos.put(p.getId(), p.getCostoUltimo());
            }
        }
        return costos;
    }
}
