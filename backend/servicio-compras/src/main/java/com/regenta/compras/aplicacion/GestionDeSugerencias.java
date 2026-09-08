package com.regenta.compras.aplicacion;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.compras.domain.EstadoSugerencia;
import com.regenta.compras.domain.Proveedor;
import com.regenta.compras.domain.SugerenciaDeCompra;
import com.regenta.compras.infra.ProveedorProductoRepositorio;
import com.regenta.compras.infra.ProveedorRepositorio;
import com.regenta.compras.infra.SugerenciaDeCompraRepositorio;
import com.regenta.comun.errores.NoEncontradoException;
import com.regenta.comun.errores.ReglaDeNegocioException;
import com.regenta.comun.negocio.ContextoDeNegocio;
import com.regenta.comun.negocio.RequierePermiso;

/**
 * La lista de reposición (HU-050). Un evento {@code stock_bajo_minimo} deja (o
 * refresca) una sugerencia por producto, con su proveedor preferido y la
 * cantidad hasta el objetivo de stock. El listado agrupa por proveedor
 * (criterio 2); aceptar un grupo crea una orden de compra en borrador con esas
 * líneas (criterio 3); un producto sin proveedor queda marcado (criterio 4).
 */
@Service
public class GestionDeSugerencias {

    private final SugerenciaDeCompraRepositorio sugerencias;
    private final ProveedorProductoRepositorio catalogo;
    private final ProveedorRepositorio proveedores;
    private final GestionDeOrdenesDeCompra ordenes;

    public GestionDeSugerencias(SugerenciaDeCompraRepositorio sugerencias,
            ProveedorProductoRepositorio catalogo, ProveedorRepositorio proveedores,
            GestionDeOrdenesDeCompra ordenes) {
        this.sugerencias = sugerencias;
        this.catalogo = catalogo;
        this.proveedores = proveedores;
        this.ordenes = ordenes;
    }

    /** Criterio 1: el producto del evento entra (o se actualiza) en la lista. */
    @Transactional
    public UUID registrarDesdeEvento(Map<String, Object> datos) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        UUID productoId = uuid(datos.get("producto_id"));
        if (productoId == null) {
            return null;
        }
        String nombre = texto(datos.get("producto_nombre"), "Producto " + productoId);
        UUID bodegaId = uuid(datos.get("bodega_id"));
        BigDecimal existencia = numero(datos.get("existencia"));
        BigDecimal minimo = numero(datos.get("minimo"));
        BigDecimal objetivo = numero(firstNonNull(datos.get("stock_maximo"),
                datos.get("stock_objetivo")));

        SugerenciaDeCompra sugerencia = sugerencias
                .findByNegocioIdAndProductoIdAndEstado(negocioId, productoId,
                        EstadoSugerencia.PENDIENTE)
                .map(existente -> {
                    existente.refrescar(nombre, bodegaId, existencia, minimo, objetivo);
                    return existente;
                })
                .orElseGet(() -> SugerenciaDeCompra.desdeStockBajo(negocioId, productoId, nombre,
                        bodegaId, existencia, minimo, objetivo));

        List<ProveedorDeProducto> deProducto = catalogo.proveedoresDelProducto(productoId);
        if (!deProducto.isEmpty()) {
            ProveedorDeProducto preferido = deProducto.get(0);
            sugerencia.asignarProveedor(preferido.proveedorId(), preferido.costoUltimo(),
                    preferido.cantidadMinima());
        }
        try {
            sugerencias.save(sugerencia);
            sugerencias.flush();
        } catch (DataIntegrityViolationException carrera) {
            SugerenciaDeCompra ya = sugerencias.findByNegocioIdAndProductoIdAndEstado(
                    negocioId, productoId, EstadoSugerencia.PENDIENTE).orElseThrow();
            ya.refrescar(nombre, bodegaId, existencia, minimo, objetivo);
            sugerencias.save(ya);
            return ya.getId();
        }
        return sugerencia.getId();
    }

    /** Criterio 2: las pendientes agrupadas por proveedor preferido. */
    @Transactional(readOnly = true)
    @RequierePermiso("COMPRAS_COMPRA_VER")
    public List<GrupoDeSugerencias> listar() {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        List<SugerenciaDeCompra> pendientes = sugerencias
                .findByNegocioIdAndEstadoOrderByNombreSnapshotAsc(negocioId,
                        EstadoSugerencia.PENDIENTE);

        Map<UUID, String> nombresProveedor = proveedores.findAllById(pendientes.stream()
                        .map(SugerenciaDeCompra::getProveedorId).filter(p -> p != null).toList())
                .stream().collect(Collectors.toMap(Proveedor::getId, Proveedor::getRazonSocial));

        Map<UUID, List<SugerenciaDeCompra>> porProveedor = new LinkedHashMap<>();
        for (SugerenciaDeCompra s : pendientes) {
            porProveedor.computeIfAbsent(s.getProveedorId(), k -> new ArrayList<>()).add(s);
        }
        List<GrupoDeSugerencias> grupos = new ArrayList<>();
        porProveedor.forEach((proveedorId, lista) -> {
            BigDecimal costo = lista.stream()
                    .map(s -> s.getCostoEstimado() == null ? BigDecimal.ZERO
                            : s.getCostoEstimado().multiply(s.getCantidadSugerida()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            grupos.add(new GrupoDeSugerencias(proveedorId,
                    proveedorId == null ? null : nombresProveedor.get(proveedorId),
                    proveedorId == null, costo,
                    lista.stream().map(SugerenciaDelNegocio::de).toList()));
        });
        // El grupo sin proveedor al final.
        grupos.sort((a, b) -> Boolean.compare(a.sinProveedor(), b.sinProveedor()));
        return grupos;
    }

    /** Criterio 3: aceptar el grupo de un proveedor crea la orden en borrador. */
    @Transactional
    @RequierePermiso("COMPRAS_COMPRA_CREAR")
    public OrdenDelNegocio aceptar(SolicitudDeAceptacion solicitud) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        List<SugerenciaDeCompra> elegidas = elegir(negocioId, solicitud);
        if (elegidas.isEmpty()) {
            throw new NoEncontradoException("No hay sugerencias pendientes para ese proveedor");
        }
        for (SugerenciaDeCompra s : elegidas) {
            if (s.isSinProveedor() || !solicitud.proveedorId().equals(s.getProveedorId())) {
                throw new ReglaDeNegocioException(
                        "Todas las sugerencias del grupo deben ser del mismo proveedor con proveedor asignado");
            }
        }
        UUID bodega = solicitud.bodegaDestinoId() != null
                ? solicitud.bodegaDestinoId()
                : elegidas.stream().map(SugerenciaDeCompra::getBodegaId)
                        .filter(b -> b != null).findFirst()
                        .orElseThrow(() -> new ReglaDeNegocioException(
                                "Indica la bodega destino de la orden"));

        List<SolicitudDeOrden.LineaDeSolicitud> lineas = elegidas.stream()
                .map(s -> new SolicitudDeOrden.LineaDeSolicitud(s.getProductoId(),
                        s.getNombreSnapshot(), s.getCantidadSugerida(), s.getCostoEstimado(),
                        null, null))
                .toList();
        OrdenDelNegocio orden = ordenes.crear(new SolicitudDeOrden(solicitud.proveedorId(),
                bodega, null, null, BigDecimal.ZERO, "Sugerencia de compra", lineas));

        for (SugerenciaDeCompra s : elegidas) {
            s.marcarEnOrden(orden.id());
            sugerencias.save(s);
        }
        return orden;
    }

    @Transactional
    @RequierePermiso("COMPRAS_COMPRA_CREAR")
    public void descartar(UUID sugerenciaId) {
        SugerenciaDeCompra s = sugerencias
                .findByIdAndNegocioId(sugerenciaId, ContextoDeNegocio.negocioActual())
                .orElseThrow(() -> new NoEncontradoException("Esa sugerencia no existe"));
        s.descartar();
        sugerencias.save(s);
    }

    private List<SugerenciaDeCompra> elegir(UUID negocioId, SolicitudDeAceptacion solicitud) {
        if (solicitud.sugerenciaIds() != null && !solicitud.sugerenciaIds().isEmpty()) {
            List<SugerenciaDeCompra> elegidas = new ArrayList<>();
            for (UUID id : solicitud.sugerenciaIds()) {
                SugerenciaDeCompra s = sugerencias.findByIdAndNegocioId(id, negocioId)
                        .orElseThrow(() -> new NoEncontradoException("Esa sugerencia no existe"));
                if (s.getEstado() != EstadoSugerencia.PENDIENTE) {
                    throw new ReglaDeNegocioException("La sugerencia ya no está pendiente");
                }
                elegidas.add(s);
            }
            return elegidas;
        }
        return sugerencias.findByNegocioIdAndProveedorIdAndEstado(negocioId,
                solicitud.proveedorId(), EstadoSugerencia.PENDIENTE);
    }

    private static Object firstNonNull(Object a, Object b) {
        return a != null ? a : b;
    }

    private static UUID uuid(Object v) {
        return v == null ? null : UUID.fromString(v.toString());
    }

    private static BigDecimal numero(Object v) {
        if (v == null) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(v.toString());
        } catch (NumberFormatException noEsNumero) {
            return BigDecimal.ZERO;
        }
    }

    private static String texto(Object v, String porDefecto) {
        return v == null || v.toString().isBlank() ? porDefecto : v.toString();
    }
}
