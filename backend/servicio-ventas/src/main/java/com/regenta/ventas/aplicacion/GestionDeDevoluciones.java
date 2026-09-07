package com.regenta.ventas.aplicacion;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.comun.errores.NoEncontradoException;
import com.regenta.comun.eventos.RegistroDeEventos;
import com.regenta.comun.negocio.ContextoDeNegocio;
import com.regenta.comun.negocio.RequierePermiso;
import com.regenta.ventas.domain.Devolucion;
import com.regenta.ventas.domain.DevolucionLinea;
import com.regenta.ventas.domain.Venta;
import com.regenta.ventas.domain.VentaLinea;
import com.regenta.ventas.infra.AsignadorDeConsecutivos;
import com.regenta.ventas.infra.DevolucionLineaRepositorio;
import com.regenta.ventas.infra.DevolucionRepositorio;
import com.regenta.ventas.infra.VentaLineaRepositorio;
import com.regenta.ventas.infra.VentaRepositorio;

/**
 * Devoluciones totales y parciales. HU-042.
 *
 * <p>Cada devolución suma a {@code venta_lineas.cantidad_devuelta} sin pasarse de
 * lo vendido (criterio 2). La venta queda {@code DEVUELTA} o
 * {@code DEVUELTA_PARCIAL} (criterio 1). Se publica {@code devolucion_registrada}
 * con las líneas, la bodega y si reintegra o no: Inventario reingresa el stock
 * solo cuando {@code reintegra_stock} es true (criterios 3 y 4). Si la venta ya
 * estaba facturada, se dispara {@code nota_credito_requerida} (criterio 5).
 */
@Service
public class GestionDeDevoluciones {

    private final VentaRepositorio ventas;
    private final VentaLineaRepositorio lineasVenta;
    private final DevolucionRepositorio devoluciones;
    private final DevolucionLineaRepositorio lineasDevolucion;
    private final AsignadorDeConsecutivos consecutivos;
    private final RegistroDeEventos eventos;

    public GestionDeDevoluciones(VentaRepositorio ventas, VentaLineaRepositorio lineasVenta,
            DevolucionRepositorio devoluciones, DevolucionLineaRepositorio lineasDevolucion,
            AsignadorDeConsecutivos consecutivos, RegistroDeEventos eventos) {
        this.ventas = ventas;
        this.lineasVenta = lineasVenta;
        this.devoluciones = devoluciones;
        this.lineasDevolucion = lineasDevolucion;
        this.consecutivos = consecutivos;
        this.eventos = eventos;
    }

    @Transactional
    @RequierePermiso("VENTAS_DEVOLUCION_REGISTRAR")
    public DevolucionDelNegocio registrar(UUID ventaId, SolicitudDeDevolucion solicitud) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        Venta venta = ventaDelNegocio(ventaId);

        long numero = consecutivos.siguiente(negocioId, "DEVOLUCION");
        Devolucion devolucion = Devolucion.crear(negocioId, ventaId, Long.toString(numero),
                solicitud.motivo(), solicitud.detalle(), solicitud.reintegraStock(),
                solicitud.bodegaDestinoId(), ContextoDeNegocio.actual().usuario());
        devoluciones.save(devolucion);

        BigDecimal total = BigDecimal.ZERO;
        boolean completa = true;
        List<Map<String, Object>> renglonesEvento = new ArrayList<>();
        java.util.Set<Short> tocadas = new java.util.HashSet<>();
        for (LineaDevuelta ld : solicitud.lineas()) {
            VentaLinea vl = lineasVenta.findByVentaIdAndLinea(ventaId, ld.linea())
                    .orElseThrow(() -> new NoEncontradoException(
                            "La linea " + ld.linea() + " no existe en la venta"));
            vl.registrarDevolucion(ld.cantidad());
            lineasVenta.save(vl);
            tocadas.add(vl.getLinea());
            if (!vl.totalmenteDevuelta()) {
                completa = false;
            }

            BigDecimal monto = vl.montoDevolucionPor(ld.cantidad());
            total = total.add(monto);
            lineasDevolucion.save(DevolucionLinea.de(negocioId, devolucion.getId(), vl.getId(),
                    vl.getProductoId(), ld.cantidad(), monto));

            Map<String, Object> renglon = new LinkedHashMap<>();
            renglon.put("producto_id", vl.getProductoId().toString());
            renglon.put("cantidad", ld.cantidad());
            renglonesEvento.add(renglon);
        }

        // Las líneas que esta devolución no tocó tienen que estar ya devueltas
        // para que la venta quede DEVUELTA y no DEVUELTA_PARCIAL.
        for (VentaLinea otra : lineasVenta.findByVentaIdOrderByLinea(ventaId)) {
            if (!tocadas.contains(otra.getLinea()) && !otra.totalmenteDevuelta()) {
                completa = false;
            }
        }
        devolucion.cerrar(total, completa);
        devoluciones.save(devolucion);
        venta.registrarDevolucion(completa);
        ventas.save(venta);

        eventos.registrar(negocioId, "Devolucion", devolucion.getId(), "devolucion_registrada",
                payload(negocioId, ventaId, devolucion, renglonesEvento));
        if (venta.estaFacturada()) {
            eventos.registrar(negocioId, "Devolucion", devolucion.getId(), "nota_credito_requerida",
                    Map.of("negocio_id", negocioId.toString(), "venta_id", ventaId.toString(),
                            "devolucion_id", devolucion.getId().toString(), "total", total));
        }

        return new DevolucionDelNegocio(devolucion.getId(), devolucion.getNumero(),
                completa ? "TOTAL" : "PARCIAL", devolucion.getMotivo(),
                devolucion.isReintegraStock(), total);
    }

    private static Map<String, Object> payload(UUID negocioId, UUID ventaId, Devolucion devolucion,
            List<Map<String, Object>> renglones) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("negocio_id", negocioId.toString());
        datos.put("venta_id", ventaId.toString());
        datos.put("devolucion_id", devolucion.getId().toString());
        datos.put("motivo", devolucion.getMotivo().name());
        datos.put("reintegra_stock", devolucion.isReintegraStock());
        datos.put("bodega_destino_id",
                devolucion.getBodegaDestinoId() == null ? null
                        : devolucion.getBodegaDestinoId().toString());
        datos.put("lineas", renglones);
        return datos;
    }

    private Venta ventaDelNegocio(UUID ventaId) {
        Venta venta = ventas.findById(ventaId)
                .orElseThrow(() -> new NoEncontradoException("Esa venta no existe"));
        if (!venta.getNegocioId().equals(ContextoDeNegocio.negocioActual())) {
            throw new NoEncontradoException("Esa venta no existe");
        }
        return venta;
    }
}
