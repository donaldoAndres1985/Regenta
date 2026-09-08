package com.regenta.compras.aplicacion;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.compras.domain.OrdenCompraLinea;
import com.regenta.compras.domain.OrdenDeCompra;
import com.regenta.compras.domain.ProveedorProducto;
import com.regenta.compras.infra.AsignadorDeConsecutivos;
import com.regenta.compras.infra.OrdenCompraLineaRepositorio;
import com.regenta.compras.infra.OrdenDeCompraRepositorio;
import com.regenta.compras.infra.ProveedorProductoRepositorio;
import com.regenta.compras.infra.ProveedorRepositorio;
import com.regenta.comun.errores.NoEncontradoException;
import com.regenta.comun.errores.ReglaDeNegocioException;
import com.regenta.comun.eventos.RegistroDeEventos;
import com.regenta.comun.negocio.ContextoDeNegocio;
import com.regenta.comun.negocio.RequierePermiso;

/**
 * Órdenes de compra con control de aprobación (HU-047).
 *
 * <p>Se arma en borrador, se aprueba —queda quién y cuándo (criterio 1), y hace
 * falta {@code COMPRAS_COMPRA_APROBAR} (criterio 2)— y recién entonces se envía.
 * Editar las líneas de una orden ya aprobada es 409 (criterio 3). Al consultarla
 * se ve, por línea, lo recibido y lo que falta (criterio 4).
 */
@Service
public class GestionDeOrdenesDeCompra {

    private static final String TIPO_CONSECUTIVO = "ORDEN_COMPRA";

    private final OrdenDeCompraRepositorio ordenes;
    private final OrdenCompraLineaRepositorio lineasRepo;
    private final ProveedorRepositorio proveedores;
    private final ProveedorProductoRepositorio catalogo;
    private final AsignadorDeConsecutivos consecutivos;
    private final RegistroDeEventos eventos;

    public GestionDeOrdenesDeCompra(OrdenDeCompraRepositorio ordenes,
            OrdenCompraLineaRepositorio lineasRepo, ProveedorRepositorio proveedores,
            ProveedorProductoRepositorio catalogo, AsignadorDeConsecutivos consecutivos,
            RegistroDeEventos eventos) {
        this.ordenes = ordenes;
        this.lineasRepo = lineasRepo;
        this.proveedores = proveedores;
        this.catalogo = catalogo;
        this.consecutivos = consecutivos;
        this.eventos = eventos;
    }

    @Transactional(readOnly = true)
    @RequierePermiso("COMPRAS_COMPRA_VER")
    public List<OrdenDelNegocio> listar() {
        return ordenes.findByNegocioIdOrderByFechaEmisionDescCreadoEnDesc(
                        ContextoDeNegocio.negocioActual())
                .stream()
                .map(o -> OrdenDelNegocio.de(o, lineasRepo.findByOrdenIdOrderByLinea(o.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    @RequierePermiso("COMPRAS_COMPRA_VER")
    public OrdenDelNegocio ver(UUID ordenId) {
        OrdenDeCompra orden = delNegocio(ordenId);
        return OrdenDelNegocio.de(orden, lineasRepo.findByOrdenIdOrderByLinea(ordenId));
    }

    @Transactional
    @RequierePermiso("COMPRAS_COMPRA_CREAR")
    public OrdenDelNegocio crear(SolicitudDeOrden solicitud) {
        UUID negocioId = ContextoDeNegocio.negocioActual();

        proveedores.findByIdAndNegocioId(solicitud.proveedorId(), negocioId)
                .filter(p -> !p.estaEliminado())
                .orElseThrow(() -> new NoEncontradoException("Ese proveedor no existe"));

        String numero = "OC-" + consecutivos.siguiente(negocioId, TIPO_CONSECUTIVO);
        OrdenDeCompra orden = OrdenDeCompra.crear(negocioId, numero, solicitud.proveedorId(),
                solicitud.bodegaDestinoId(), solicitud.sucursalId(),
                ContextoDeNegocio.actual().usuario(), solicitud.fechaEsperada(),
                solicitud.flete(), limpiar(solicitud.observaciones()));

        List<OrdenCompraLinea> lineas = construirLineas(orden, negocioId,
                solicitud.proveedorId(), solicitud.lineas());
        orden.totalizar(lineas);

        ordenes.save(orden);
        lineasRepo.saveAll(lineas);
        return OrdenDelNegocio.de(orden, lineas);
    }

    /** Criterio 3: si la orden no está en borrador, esto revienta con 409. */
    @Transactional
    @RequierePermiso("COMPRAS_COMPRA_CREAR")
    public OrdenDelNegocio editarLineas(UUID ordenId,
            List<SolicitudDeOrden.LineaDeSolicitud> nuevas) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        OrdenDeCompra orden = delNegocio(ordenId);
        orden.exigirBorradorParaEditar();

        lineasRepo.borrarDeLaOrden(ordenId);
        lineasRepo.flush();

        List<OrdenCompraLinea> lineas = construirLineas(orden, negocioId,
                orden.getProveedorId(), nuevas);
        orden.totalizar(lineas);

        ordenes.save(orden);
        lineasRepo.saveAll(lineas);
        return OrdenDelNegocio.de(orden, lineas);
    }

    /** Criterios 1 y 2. */
    @Transactional
    @RequierePermiso("COMPRAS_COMPRA_APROBAR")
    public OrdenDelNegocio aprobar(UUID ordenId) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        OrdenDeCompra orden = delNegocio(ordenId);
        orden.aprobar(ContextoDeNegocio.actual().usuario(), lineasRepo.existsByOrdenId(ordenId));
        ordenes.save(orden);

        eventos.registrar(negocioId, "OrdenDeCompra", orden.getId(), "orden_compra_aprobada",
                payloadAprobada(negocioId, orden));
        return OrdenDelNegocio.de(orden, lineasRepo.findByOrdenIdOrderByLinea(ordenId));
    }

    @Transactional
    @RequierePermiso("COMPRAS_COMPRA_CREAR")
    public OrdenDelNegocio enviar(UUID ordenId) {
        OrdenDeCompra orden = delNegocio(ordenId);
        orden.enviar();
        ordenes.save(orden);
        return OrdenDelNegocio.de(orden, lineasRepo.findByOrdenIdOrderByLinea(ordenId));
    }

    private List<OrdenCompraLinea> construirLineas(OrdenDeCompra orden, UUID negocioId,
            UUID proveedorId, List<SolicitudDeOrden.LineaDeSolicitud> solicitadas) {
        List<OrdenCompraLinea> lineas = new ArrayList<>();
        int n = 1;
        for (SolicitudDeOrden.LineaDeSolicitud l : solicitadas) {
            lineas.add(OrdenCompraLinea.nueva(orden.getId(), negocioId, n++, l.productoId(),
                    l.nombre().trim(), l.cantidad(), costoDe(proveedorId, l),
                    l.descuentoPct(), l.impuestoPct()));
        }
        return lineas;
    }

    /** El costo de la línea: el que vino, o el que el proveedor tiene para ese producto. */
    private BigDecimal costoDe(UUID proveedorId, SolicitudDeOrden.LineaDeSolicitud l) {
        if (l.costoUnitario() != null) {
            return l.costoUnitario();
        }
        return catalogo.findByProveedorIdAndProductoId(proveedorId, l.productoId())
                .map(ProveedorProducto::getCostoUltimo)
                .orElseThrow(() -> new ReglaDeNegocioException(
                        "La línea del producto " + l.productoId()
                                + " no trae costo y el proveedor no tiene uno registrado"));
    }

    private OrdenDeCompra delNegocio(UUID ordenId) {
        return ordenes.findByIdAndNegocioId(ordenId, ContextoDeNegocio.negocioActual())
                .orElseThrow(() -> new NoEncontradoException("Esa orden no existe"));
    }

    private static Map<String, Object> payloadAprobada(UUID negocioId, OrdenDeCompra orden) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("negocio_id", negocioId.toString());
        datos.put("orden_id", orden.getId().toString());
        datos.put("numero", orden.getNumero());
        datos.put("proveedor_id", orden.getProveedorId().toString());
        datos.put("bodega_destino_id", orden.getBodegaDestinoId().toString());
        datos.put("total", orden.getTotal());
        datos.put("aprobado_por", String.valueOf(orden.getAprobadoPor()));
        return datos;
    }

    private static String limpiar(String valor) {
        if (valor == null) {
            return null;
        }
        String recortado = valor.trim();
        return recortado.isEmpty() ? null : recortado;
    }
}
