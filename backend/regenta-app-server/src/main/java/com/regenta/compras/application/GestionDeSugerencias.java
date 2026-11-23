package com.regenta.compras.application;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.compras.domain.EstadoSugerencia;
import com.regenta.compras.domain.Proveedor;
import com.regenta.compras.domain.ProveedorProducto;
import com.regenta.compras.domain.SugerenciaDeCompra;
import com.regenta.compras.infrastructure.ProveedorProductoRepositorio;
import com.regenta.compras.infrastructure.ProveedorRepositorio;
import com.regenta.compras.infrastructure.SugerenciaDeCompraRepositorio;
import com.regenta.shared.web.NoEncontradoException;
import com.regenta.shared.web.ReglaDeNegocioException;
import com.regenta.shared.tenancy.ContextoDeNegocio;
import com.regenta.shared.security.RequierePermiso;

/**
 * La lista de reposición (HU-050, HU-181). Un evento {@code stock_bajo_minimo} deja (o
 * refresca) <b>una</b> sugerencia pendiente por producto y bodega, con su proveedor preferido y
 * la cantidad hasta el objetivo de stock redondeada al múltiplo que vende el proveedor (I30). El
 * listado agrupa por proveedor y bodega; aceptar un grupo crea una orden de compra en borrador
 * con esas líneas, y un producto sin proveedor queda marcado para asignarle uno (I31).
 */
@Service
public class GestionDeSugerencias {

    private final SugerenciaDeCompraRepositorio sugerencias;
    private final ProveedorProductoRepositorio catalogo;
    private final ProveedorRepositorio proveedores;
    private final GestionDeOrdenesDeCompra ordenes;
    private final GestionDeProveedores gestionDeProveedores;

    public GestionDeSugerencias(SugerenciaDeCompraRepositorio sugerencias,
            ProveedorProductoRepositorio catalogo, ProveedorRepositorio proveedores,
            GestionDeOrdenesDeCompra ordenes, GestionDeProveedores gestionDeProveedores) {
        this.sugerencias = sugerencias;
        this.catalogo = catalogo;
        this.proveedores = proveedores;
        this.ordenes = ordenes;
        this.gestionDeProveedores = gestionDeProveedores;
    }

    /** I30 · El producto del evento entra (o se actualiza) en la lista de esa bodega. */
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
                .findByNegocioIdAndProductoIdAndBodegaIdAndEstado(negocioId, productoId, bodegaId,
                        EstadoSugerencia.PENDIENTE)
                .map(existente -> {
                    existente.refrescar(nombre, bodegaId, existencia, minimo, objetivo);
                    return existente;
                })
                .orElseGet(() -> SugerenciaDeCompra.desdeStockBajo(negocioId, productoId, nombre,
                        bodegaId, existencia, minimo, objetivo));
        conSuProveedor(sugerencia);
        try {
            sugerencias.save(sugerencia);
            sugerencias.flush();
        } catch (DataIntegrityViolationException carrera) {
            SugerenciaDeCompra ya = sugerencias.findByNegocioIdAndProductoIdAndBodegaIdAndEstado(
                    negocioId, productoId, bodegaId, EstadoSugerencia.PENDIENTE).orElseThrow();
            ya.refrescar(nombre, bodegaId, existencia, minimo, objetivo);
            conSuProveedor(ya);
            sugerencias.save(ya);
            return ya.getId();
        }
        return sugerencia.getId();
    }

    /** El preferido del producto (o, si no hay, el primero que lo vende) con su costo y su mínimo. */
    private void conSuProveedor(SugerenciaDeCompra sugerencia) {
        List<ProveedorDeProducto> deProducto = catalogo.proveedoresDelProducto(sugerencia.getProductoId());
        if (!deProducto.isEmpty()) {
            ProveedorDeProducto preferido = deProducto.get(0);
            sugerencia.asignarProveedor(preferido.proveedorId(), preferido.costoUltimo(),
                    preferido.cantidadMinima());
        }
    }

    /** I31 · Las pendientes agrupadas por proveedor y bodega; «Sin proveedor» al final. */
    @Transactional(readOnly = true)
    @RequierePermiso("COMPRAS_COMPRA_VER")
    public List<GrupoDeSugerencias> listar() {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        List<SugerenciaDeCompra> pendientes = sugerencias
                .findByNegocioIdAndEstadoOrderByNombreSnapshotAsc(negocioId,
                        EstadoSugerencia.PENDIENTE);

        Map<UUID, String> nombresProveedor = proveedores.findAllById(pendientes.stream()
                        .map(SugerenciaDeCompra::getProveedorId).filter(Objects::nonNull).distinct().toList())
                .stream().collect(Collectors.toMap(Proveedor::getId, Proveedor::getRazonSocial));

        Map<List<UUID>, List<SugerenciaDeCompra>> porGrupo = new LinkedHashMap<>();
        for (SugerenciaDeCompra s : pendientes) {
            List<UUID> llave = s.isSinProveedor()
                    ? java.util.Arrays.asList(null, null)
                    : java.util.Arrays.asList(s.getProveedorId(), s.getBodegaId());
            porGrupo.computeIfAbsent(llave, k -> new ArrayList<>()).add(s);
        }
        List<GrupoDeSugerencias> grupos = new ArrayList<>();
        porGrupo.forEach((llave, lista) -> {
            UUID proveedorId = llave.get(0);
            BigDecimal costo = lista.stream()
                    .map(s -> s.getCostoEstimado() == null ? BigDecimal.ZERO
                            : s.getCostoEstimado().multiply(s.getCantidadSugerida()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            grupos.add(new GrupoDeSugerencias(proveedorId,
                    proveedorId == null ? null : nombresProveedor.get(proveedorId), llave.get(1),
                    proveedorId == null, costo,
                    lista.stream().map(SugerenciaDelNegocio::de).toList()));
        });
        grupos.sort((a, b) -> {
            if (a.sinProveedor() != b.sinProveedor()) {
                return Boolean.compare(a.sinProveedor(), b.sinProveedor());
            }
            return String.CASE_INSENSITIVE_ORDER.compare(
                    Objects.toString(a.proveedorRazonSocial(), ""), Objects.toString(b.proveedorRazonSocial(), ""));
        });
        return grupos;
    }

    /** I31 · Aceptar el grupo de un proveedor crea una orden en borrador y las pasa a EN_ORDEN. */
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
        List<UUID> bodegas = elegidas.stream().map(SugerenciaDeCompra::getBodegaId)
                .filter(Objects::nonNull).distinct().toList();
        UUID bodega = solicitud.bodegaDestinoId();
        if (bodega == null) {
            if (bodegas.size() > 1) {
                throw new ReglaDeNegocioException(
                        "Las sugerencias son de varias bodegas: indica la bodega destino de la orden");
            }
            bodega = bodegas.stream().findFirst().orElseThrow(() -> new ReglaDeNegocioException(
                    "Indica la bodega destino de la orden"));
        }

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

    /**
     * HU-181 C5 · Le asigna un proveedor a un producto de «Sin proveedor»: queda como su preferido
     * en el catálogo (con lo que ya tuviera ahí: código, costo, mínimo) y todas las sugerencias
     * pendientes del producto pasan a ese proveedor, con la cantidad redondeada a su mínimo.
     */
    @Transactional
    @RequierePermiso("COMPRAS_COMPRA_CREAR")
    public SugerenciaDelNegocio asignarProveedor(UUID sugerenciaId, SolicitudDeProveedorParaSugerencia solicitud) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        SugerenciaDeCompra sugerencia = sugerencias.findByIdAndNegocioId(sugerenciaId, negocioId)
                .orElseThrow(() -> new NoEncontradoException("Esa sugerencia no existe"));
        if (sugerencia.getEstado() != EstadoSugerencia.PENDIENTE) {
            throw new ReglaDeNegocioException("La sugerencia ya no está pendiente");
        }
        UUID productoId = sugerencia.getProductoId();
        ProveedorProducto actual = catalogo
                .findByProveedorIdAndProductoId(solicitud.proveedorId(), productoId).orElse(null);
        ProductoDeProveedor enCatalogo = gestionDeProveedores.asociarProducto(solicitud.proveedorId(),
                new SolicitudDeProductoDeProveedor(productoId,
                        actual == null ? null : actual.getCodigoProveedor(),
                        actual == null ? null : actual.getCostoUltimo(),
                        actual == null || actual.getDiasEntrega() == null ? null : actual.getDiasEntrega().intValue(),
                        actual == null ? null : actual.getCantidadMinima(),
                        true));
        for (SugerenciaDeCompra s : sugerencias.findByNegocioIdAndProductoIdAndEstado(negocioId,
                productoId, EstadoSugerencia.PENDIENTE)) {
            s.asignarProveedor(solicitud.proveedorId(), enCatalogo.costoUltimo(), enCatalogo.cantidadMinima());
            sugerencias.save(s);
        }
        return SugerenciaDelNegocio.de(sugerencia);
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
        List<SugerenciaDeCompra> delProveedor = sugerencias.findByNegocioIdAndProveedorIdAndEstado(
                negocioId, solicitud.proveedorId(), EstadoSugerencia.PENDIENTE);
        if (solicitud.bodegaDestinoId() == null) {
            return delProveedor;
        }
        return delProveedor.stream()
                .filter(s -> s.getBodegaId() == null || solicitud.bodegaDestinoId().equals(s.getBodegaId()))
                .toList();
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
