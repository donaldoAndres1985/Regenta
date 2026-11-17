package com.regenta.inventario.application;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.inventario.domain.Producto;
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

    public GestionDeFracciones(TablaDeFraccionesRepositorio tablas, ProductoRepositorio productos) {
        this.tablas = tablas;
        this.productos = productos;
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
        Producto p = productos.findById(productoId).filter(x -> x.getNegocioId().equals(negocioId))
                .orElseThrow(() -> new NoEncontradoException("Ese producto no existe"));
        if (p.getTablaFraccionesId() == null) {
            return List.of();
        }
        return tablas.findById(p.getTablaFraccionesId()).map(t -> t.getFracciones().stream()
                .map(f -> new FraccionDelNegocio(f.getMedida(), f.getFactor(), f.getPrecio(), "TABLA"))
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
