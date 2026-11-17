package com.regenta.inventario.application;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.inventario.domain.PrecioPropioDeFraccion;
import com.regenta.inventario.domain.Producto;
import com.regenta.inventario.infrastructure.PrecioPropioDeFraccionRepositorio;
import com.regenta.inventario.domain.TablaDeFracciones;
import com.regenta.inventario.infrastructure.ProductoRepositorio;
import com.regenta.inventario.infrastructure.TablaDeFraccionesRepositorio;
import com.regenta.shared.security.RequierePermiso;
import com.regenta.shared.tenancy.ContextoDeNegocio;
import com.regenta.shared.web.ConflictoDeEstadoException;
import com.regenta.shared.web.NoEncontradoException;
import com.regenta.shared.web.RecursoDuplicadoException;

/**
 * HU-156 · tablas de fracciones compartidas: se arma una tabla con precios fijos y se le asignan
 * los productos que la usan. Cambiar un precio de la tabla lo cambia para todos (I14).
 */
@Service
public class GestionDeFracciones {

    private final TablaDeFraccionesRepositorio tablas;
    private final ProductoRepositorio productos;
    private final PrecioPropioDeFraccionRepositorio propios;

    public GestionDeFracciones(TablaDeFraccionesRepositorio tablas, ProductoRepositorio productos,
            PrecioPropioDeFraccionRepositorio propios) {
        this.tablas = tablas;
        this.productos = productos;
        this.propios = propios;
    }

    /** HU-157 C1 · el precio de una fracción para este producto; le gana al de la tabla. */
    @Transactional
    @RequierePermiso("INVENTARIO_FRACCION_GESTIONAR")
    public void fijarPrecioPropio(UUID productoId, String medida, java.math.BigDecimal precio) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        Producto p = productoDelNegocio(negocioId, productoId);
        boolean enLaTabla = p.getTablaFraccionesId() != null && tablas.findById(p.getTablaFraccionesId())
                .map(t -> t.precioDe(medida) != null).orElse(false);
        if (!enLaTabla) {
            throw new com.regenta.shared.web.ReglaDeNegocioException(
                    "«" + medida + "» no está en la tabla de fracciones de este producto.");
        }
        PrecioPropioDeFraccion propio = propios.findByProductoIdAndMedida(productoId, medida)
                .orElseGet(() -> PrecioPropioDeFraccion.de(negocioId, productoId, medida, precio));
        propio.cambiarPrecio(precio);
        propios.save(propio);
    }

    /** HU-157 C3 · vuelve al precio de la tabla. */
    @Transactional
    @RequierePermiso("INVENTARIO_FRACCION_GESTIONAR")
    public void quitarPrecioPropio(UUID productoId, String medida) {
        productoDelNegocio(ContextoDeNegocio.negocioActual(), productoId);
        propios.findByProductoIdAndMedida(productoId, medida).ifPresent(propios::delete);
    }

    private Producto productoDelNegocio(UUID negocioId, UUID productoId) {
        return productos.findById(productoId).filter(x -> x.getNegocioId().equals(negocioId))
                .orElseThrow(() -> new NoEncontradoException("Ese producto no existe"));
    }

    @Transactional(readOnly = true)
    @RequierePermiso("INVENTARIO_PRODUCTO_VER")
    public List<TablaDeFraccionesDelNegocio> listarTablas() {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        return tablas.findByNegocioIdOrderByNombre(negocioId).stream()
                .map(t -> TablaDeFraccionesDelNegocio.de(t,
                        productos.countByNegocioIdAndTablaFraccionesId(negocioId, t.getId())))
                .toList();
    }

    @Transactional
    @RequierePermiso("INVENTARIO_FRACCION_GESTIONAR")
    public TablaDeFraccionesDelNegocio crearTabla(SolicitudDeTablaDeFracciones solicitud) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        if (tablas.existsByNegocioIdAndNombreIgnoreCase(negocioId, solicitud.nombre().trim())) {
            throw new RecursoDuplicadoException("Ya hay una tabla llamada «" + solicitud.nombre().trim() + "».");
        }
        TablaDeFracciones t = TablaDeFracciones.nueva(negocioId, solicitud.nombre(), definiciones(solicitud));
        return TablaDeFraccionesDelNegocio.de(tablas.save(t), 0);
    }

    /** HU-156 C4 · cambiar un precio aquí lo cambia para todos los productos de la tabla. */
    @Transactional
    @RequierePermiso("INVENTARIO_FRACCION_GESTIONAR")
    public TablaDeFraccionesDelNegocio editarTabla(UUID tablaId, SolicitudDeTablaDeFracciones solicitud) {
        TablaDeFracciones t = tablaDelNegocio(tablaId);
        t.renombrar(solicitud.nombre());
        t.reemplazarFracciones(definiciones(solicitud));
        return TablaDeFraccionesDelNegocio.de(tablas.save(t),
                productos.countByNegocioIdAndTablaFraccionesId(t.getNegocioId(), tablaId));
    }

    /** HU-156 C3/C5/C6 · todo o nada; cambiar a alguien de tabla hay que confirmarlo. */
    @Transactional
    @RequierePermiso("INVENTARIO_FRACCION_GESTIONAR")
    public int asignar(UUID tablaId, AsignacionDeTabla asignacion) {
        TablaDeFracciones tabla = tablaDelNegocio(tablaId);
        UUID negocioId = tabla.getNegocioId();
        List<Producto> elegidos = productos.findAllById(asignacion.productoIds()).stream()
                .filter(p -> p.getNegocioId().equals(negocioId)).toList();
        if (elegidos.size() != new HashSet<>(asignacion.productoIds()).size()) {
            throw new NoEncontradoException("Alguno de los productos no existe; no se asignó ninguno.");
        }
        long conOtra = elegidos.stream()
                .filter(p -> p.getTablaFraccionesId() != null && !p.getTablaFraccionesId().equals(tablaId))
                .count();
        if (conOtra > 0 && !asignacion.confirmarCambio()) {
            throw new ConflictoDeEstadoException(conOtra + " de estos productos ya tiene"
                    + (conOtra == 1 ? "" : "n") + " otra tabla de fracciones; confirma el cambio.");
        }
        elegidos.forEach(p -> p.asignarTablaDeFracciones(tablaId));
        productos.saveAll(elegidos);
        return elegidos.size();
    }

    /** HU-156/157 · las fracciones con las que se vende un producto (I14). */
    @Transactional(readOnly = true)
    @RequierePermiso("INVENTARIO_PRODUCTO_VER")
    public List<FraccionDelNegocio> fraccionesDe(UUID productoId) {
        return fraccionesSinPermiso(ContextoDeNegocio.negocioActual(), productoId);
    }

    /** Para Ventas (por {@code InventarioApi}): quien vende no necesita ver el catálogo. */
    @Transactional(readOnly = true)
    public List<FraccionDelNegocio> fraccionesSinPermiso(UUID negocioId, UUID productoId) {
        Producto p = productoDelNegocio(negocioId, productoId);
        if (p.getTablaFraccionesId() == null) {
            return List.of();
        }
        // HU-157 · I14: el precio propio le gana al de la tabla.
        java.util.Map<String, java.math.BigDecimal> propiosDe = new java.util.HashMap<>();
        propios.findByProductoId(productoId).forEach(x -> propiosDe.put(x.getMedida(), x.getPrecio()));
        return tablas.findById(p.getTablaFraccionesId()).map(t -> t.getFracciones().stream()
                .map(f -> propiosDe.containsKey(f.getMedida())
                        ? new FraccionDelNegocio(f.getMedida(), f.getFactor(), propiosDe.get(f.getMedida()), "PROPIO")
                        : new FraccionDelNegocio(f.getMedida(), f.getFactor(), f.getPrecio(), "TABLA"))
                .toList()).orElse(List.of());
    }

    private TablaDeFracciones tablaDelNegocio(UUID tablaId) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        return tablas.findById(tablaId).filter(t -> t.getNegocioId().equals(negocioId))
                .orElseThrow(() -> new NoEncontradoException("Esa tabla de fracciones no existe"));
    }

    private static List<TablaDeFracciones.Definicion> definiciones(SolicitudDeTablaDeFracciones s) {
        return s.fracciones().stream()
                .map(l -> new TablaDeFracciones.Definicion(l.medida(), l.factor(), l.precio()))
                .toList();
    }
}
