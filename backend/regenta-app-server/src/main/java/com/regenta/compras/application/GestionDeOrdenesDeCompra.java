package com.regenta.compras.application;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.compras.domain.EstadoOrdenCompra;
import com.regenta.compras.domain.OrdenCompraLinea;
import com.regenta.compras.domain.OrdenDeCompra;
import com.regenta.compras.domain.ProveedorProducto;
import com.regenta.compras.infrastructure.AsignadorDeConsecutivos;
import com.regenta.compras.infrastructure.OrdenCompraLineaRepositorio;
import com.regenta.compras.infrastructure.OrdenDeCompraRepositorio;
import com.regenta.compras.infrastructure.ProveedorProductoRepositorio;
import com.regenta.compras.infrastructure.ProveedorRepositorio;
import com.regenta.inventario.api.InventarioApi;
import com.regenta.inventario.api.ProductoParaComprar;
import com.regenta.shared.web.ConflictoDeEstadoException;
import com.regenta.shared.web.NoEncontradoException;
import com.regenta.usuarios.api.ConfiguracionNegocioApi;
import com.regenta.shared.web.ReglaDeNegocioException;
import com.regenta.shared.events.RegistroDeEventos;
import com.regenta.shared.tenancy.ContextoDeNegocio;
import com.regenta.shared.security.RequierePermiso;

/**
 * Ã“rdenes de compra con control de aprobaciÃ³n (HU-047).
 *
 * <p>Se arma en borrador, se aprueba â€”queda quiÃ©n y cuÃ¡ndo (criterio 1), y hace
 * falta {@code COMPRAS_COMPRA_APROBAR} (criterio 2)â€” y reciÃ©n entonces se envÃ­a.
 * Editar las lÃ­neas de una orden ya aprobada es 409 (criterio 3). Al consultarla
 * se ve, por lÃ­nea, lo recibido y lo que falta (criterio 4).
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
    private final GestionDeConfiguracionDeCompras configuracion;
    private final InventarioApi inventario;
    private final ConfiguracionNegocioApi negocio;

    public GestionDeOrdenesDeCompra(OrdenDeCompraRepositorio ordenes,
            OrdenCompraLineaRepositorio lineasRepo, ProveedorRepositorio proveedores,
            ProveedorProductoRepositorio catalogo, AsignadorDeConsecutivos consecutivos,
            RegistroDeEventos eventos, GestionDeConfiguracionDeCompras configuracion,
            InventarioApi inventario, @Autowired(required = false) ConfiguracionNegocioApi negocio) {
        this.configuracion = configuracion;
        this.inventario = inventario;
        this.negocio = negocio;
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
                .map(o -> vista(o, lineasRepo.findByOrdenIdOrderByLinea(o.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    @RequierePermiso("COMPRAS_COMPRA_VER")
    public OrdenDelNegocio ver(UUID ordenId) {
        OrdenDeCompra orden = delNegocio(ordenId);
        return vista(orden, lineasRepo.findByOrdenIdOrderByLinea(ordenId));
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
        return vista(orden, lineas);
    }

    /** Criterio 3: si la orden no estÃ¡ en borrador, esto revienta con 409. */
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
        return vista(orden, lineas);
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
        return vista(orden, lineasRepo.findByOrdenIdOrderByLinea(ordenId));
    }

    /**
     * HU-173 C2 / C3 · Se terminó de armar la orden. Si el negocio no exige aprobación —o la orden
     * queda por debajo del monto mínimo— se aprueba sola a nombre de quien la confirma; si no, queda
     * esperando que alguien con permiso la apruebe.
     */
    @Transactional
    @RequierePermiso("COMPRAS_COMPRA_CREAR")
    public OrdenDelNegocio confirmar(UUID ordenId) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        OrdenDeCompra orden = delNegocio(ordenId);
        boolean requiere = configuracion.deNegocio(negocioId).requiereAprobacion(orden.getTotal());
        orden.confirmar(ContextoDeNegocio.actual().usuario(), lineasRepo.existsByOrdenId(ordenId), requiere);
        ordenes.save(orden);
        if (orden.isAprobacionAutomatica()) {
            eventos.registrar(negocioId, "OrdenDeCompra", orden.getId(), "orden_compra_aprobada",
                    payloadAprobada(negocioId, orden));
        }
        return vista(orden, lineasRepo.findByOrdenIdOrderByLinea(ordenId));
    }

    @Transactional
    @RequierePermiso("COMPRAS_COMPRA_CREAR")
    public OrdenDelNegocio enviar(UUID ordenId) {
        OrdenDeCompra orden = delNegocio(ordenId);
        orden.enviar();
        ordenes.save(orden);
        return vista(orden, lineasRepo.findByOrdenIdOrderByLinea(ordenId));
    }

    private List<OrdenCompraLinea> construirLineas(OrdenDeCompra orden, UUID negocioId,
            UUID proveedorId, List<SolicitudDeOrden.LineaDeSolicitud> solicitadas) {
        List<OrdenCompraLinea> lineas = new ArrayList<>();
        int n = 1;
        // HU-174 C2 / I8: el nombre y la tarifa de IVA los dice el catálogo, no quien arma la orden.
        java.util.Map<UUID, ProductoParaComprar> productos = inventario.paraComprar(negocioId,
                solicitadas.stream().map(SolicitudDeOrden.LineaDeSolicitud::productoId).toList());
        for (SolicitudDeOrden.LineaDeSolicitud l : solicitadas) {
            ProductoParaComprar p = productos.get(l.productoId());
            if (p == null) {
                throw new NoEncontradoException("Ese producto no existe en el catálogo");
            }
            if (!p.activo()) {
                throw new ReglaDeNegocioException("El producto «" + p.nombre()
                        + "» está desactivado: reactívalo en Inventario para pedirlo");
            }
            lineas.add(OrdenCompraLinea.nueva(orden.getId(), negocioId, n++, l.productoId(),
                    p.nombre(), l.cantidad(), costoDe(proveedorId, l),
                    l.descuentoPct(), p.impuestoPct()));
        }
        return lineas;
    }

    /** El costo de la lÃ­nea: el que vino, o el que el proveedor tiene para ese producto. */
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

    /**
     * HU-174 C4 · El PDF de la orden para el proveedor. Solo de una orden aprobada (o ya enviada o en
     * recepción): un borrador todavía puede cambiar.
     */
    @Transactional(readOnly = true)
    @RequierePermiso("COMPRAS_COMPRA_VER")
    public byte[] pdf(UUID ordenId) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        OrdenDeCompra orden = delNegocio(ordenId);
        if (orden.getEstado() == EstadoOrdenCompra.BORRADOR || orden.getEstado() == EstadoOrdenCompra.ANULADA) {
            throw new ConflictoDeEstadoException("Una orden " + orden.getEstado()
                    + " no se le manda al proveedor: primero tiene que estar aprobada");
        }
        var proveedor = proveedores.findByIdAndNegocioId(orden.getProveedorId(), negocioId).orElse(null);
        OrdenDelNegocio vista = vista(orden, lineasRepo.findByOrdenIdOrderByLinea(ordenId));
        return new DocumentoDeOrdenDeCompra().generar(negocio == null ? null : negocio.datosTicket(negocioId), vista,
                proveedor == null ? null : proveedor.getRazonSocial(),
                proveedor == null ? null : proveedor.getNumeroDocumento(),
                proveedor == null ? null : unir(proveedor.getContactoNombre(), proveedor.getTelefono()));
    }

    /** HU-174 · La orden con el nombre del proveedor y el código con que vende cada producto. */
    private OrdenDelNegocio vista(OrdenDeCompra orden, List<OrdenCompraLinea> lineas) {
        UUID negocioId = orden.getNegocioId();
        String nombre = proveedores.findByIdAndNegocioId(orden.getProveedorId(), negocioId)
                .map(p -> p.getNombreComercial() != null ? p.getNombreComercial() : p.getRazonSocial())
                .orElse(null);
        Map<UUID, String> codigos = new java.util.HashMap<>();
        for (ProveedorProducto pp : catalogo.findByProveedorId(orden.getProveedorId())) {
            if (pp.getCodigoProveedor() != null) {
                codigos.put(pp.getProductoId(), pp.getCodigoProveedor());
            }
        }
        return OrdenDelNegocio.de(orden, lineas, nombre, codigos);
    }

    private static String unir(String a, String b) {
        String x = a == null ? "" : a.trim();
        String y = b == null ? "" : b.trim();
        return x.isEmpty() ? (y.isEmpty() ? null : y) : (y.isEmpty() ? x : x + " · " + y);
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
