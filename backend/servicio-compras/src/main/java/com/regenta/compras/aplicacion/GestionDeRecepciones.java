package com.regenta.compras.aplicacion;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.compras.domain.CuentaPorPagar;
import com.regenta.compras.domain.OrdenCompraLinea;
import com.regenta.compras.domain.OrdenDeCompra;
import com.regenta.compras.domain.Proveedor;
import com.regenta.compras.domain.Recepcion;
import com.regenta.compras.domain.RecepcionLinea;
import com.regenta.compras.infra.AsignadorDeConsecutivos;
import com.regenta.compras.infra.CuentaPorPagarRepositorio;
import com.regenta.compras.infra.OrdenCompraLineaRepositorio;
import com.regenta.compras.infra.OrdenDeCompraRepositorio;
import com.regenta.compras.infra.ProveedorRepositorio;
import com.regenta.compras.infra.RecepcionLineaRepositorio;
import com.regenta.compras.infra.RecepcionRepositorio;
import com.regenta.comun.errores.ConflictoDeEstadoException;
import com.regenta.comun.errores.NoEncontradoException;
import com.regenta.comun.eventos.RegistroDeEventos;
import com.regenta.comun.negocio.ContextoDeNegocio;
import com.regenta.comun.negocio.RequierePermiso;

/**
 * Recepción de mercancía de una orden de compra (HU-048).
 *
 * <p>Se arma en borrador con lo que llegó —lote y vencimiento donde la categoría
 * lo exige, si no se captura es 422 (criterio 1)— y al confirmarla: se suma a lo
 * recibido de cada línea de la orden (nunca por encima de lo pedido + 5%,
 * criterio 4), la orden queda {@code PARCIAL} o {@code RECIBIDA} (criterio 5),
 * se publica {@code recepcion_registrada} para que Inventario dé entrada
 * (criterio 3) y —si trae factura— se abre la cuenta por pagar con el
 * vencimiento del plazo del proveedor (criterio 6).
 */
@Service
public class GestionDeRecepciones {

    private static final String TIPO_CONSECUTIVO = "RECEPCION";

    private final RecepcionRepositorio recepciones;
    private final RecepcionLineaRepositorio recepcionLineas;
    private final OrdenDeCompraRepositorio ordenes;
    private final OrdenCompraLineaRepositorio ordenLineas;
    private final CuentaPorPagarRepositorio cuentas;
    private final ProveedorRepositorio proveedores;
    private final AsignadorDeConsecutivos consecutivos;
    private final RegistroDeEventos eventos;

    public GestionDeRecepciones(RecepcionRepositorio recepciones,
            RecepcionLineaRepositorio recepcionLineas, OrdenDeCompraRepositorio ordenes,
            OrdenCompraLineaRepositorio ordenLineas, CuentaPorPagarRepositorio cuentas,
            ProveedorRepositorio proveedores, AsignadorDeConsecutivos consecutivos,
            RegistroDeEventos eventos) {
        this.recepciones = recepciones;
        this.recepcionLineas = recepcionLineas;
        this.ordenes = ordenes;
        this.ordenLineas = ordenLineas;
        this.cuentas = cuentas;
        this.proveedores = proveedores;
        this.consecutivos = consecutivos;
        this.eventos = eventos;
    }

    @Transactional(readOnly = true)
    @RequierePermiso("COMPRAS_COMPRA_VER")
    public List<RecepcionDelNegocio> listar(UUID ordenId) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        List<Recepcion> filas = ordenId == null
                ? recepciones.findByNegocioIdOrderByFechaDesc(negocioId)
                : recepciones.findByNegocioIdAndOrdenIdOrderByFechaDesc(negocioId, ordenId);
        return filas.stream()
                .map(r -> RecepcionDelNegocio.de(r, recepcionLineas.findByRecepcionId(r.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    @RequierePermiso("COMPRAS_COMPRA_VER")
    public RecepcionDelNegocio ver(UUID recepcionId) {
        Recepcion r = delNegocio(recepcionId);
        return RecepcionDelNegocio.de(r, recepcionLineas.findByRecepcionId(recepcionId));
    }

    @Transactional
    @RequierePermiso("COMPRAS_COMPRA_CREAR")
    public RecepcionDelNegocio crear(SolicitudDeRecepcion solicitud) {
        UUID negocioId = ContextoDeNegocio.negocioActual();

        OrdenDeCompra orden = ordenes.findByIdAndNegocioId(solicitud.ordenId(), negocioId)
                .orElseThrow(() -> new NoEncontradoException("Esa orden no existe"));
        orden.exigirRecibible();

        Map<UUID, OrdenCompraLinea> lineasOrden = ordenLineas
                .findByOrdenIdOrderByLinea(orden.getId()).stream()
                .collect(Collectors.toMap(OrdenCompraLinea::getId, Function.identity()));

        String numero = "REC-" + consecutivos.siguiente(negocioId, TIPO_CONSECUTIVO);
        UUID bodega = solicitud.bodegaId() != null
                ? solicitud.bodegaId() : orden.getBodegaDestinoId();
        Recepcion recepcion = Recepcion.crear(negocioId, numero, orden.getId(),
                orden.getProveedorId(), bodega, solicitud.facturaProveedor(),
                ContextoDeNegocio.actual().usuario(), solicitud.observaciones());

        List<RecepcionLinea> lineas = new ArrayList<>();
        for (SolicitudDeRecepcion.LineaDeRecepcion l : solicitud.lineas()) {
            OrdenCompraLinea ol = lineasOrden.get(l.ordenLineaId());
            if (ol == null) {
                throw new NoEncontradoException("La línea " + l.ordenLineaId()
                        + " no pertenece a la orden");
            }
            BigDecimal costo = l.costoUnitario() != null ? l.costoUnitario() : ol.getCostoUnitario();
            lineas.add(RecepcionLinea.nueva(recepcion.getId(), negocioId, ol.getId(),
                    ol.getProductoId(), l.cantidad(), costo, l.exigeLote(), l.codigoLote(),
                    l.fechaVencimiento(), l.registroSanitario()));
        }
        recepcion.totalizar(lineas);

        recepciones.save(recepcion);
        recepcionLineas.saveAll(lineas);
        return RecepcionDelNegocio.de(recepcion, lineas);
    }

    @Transactional
    @RequierePermiso("COMPRAS_COMPRA_CREAR")
    public RecepcionDelNegocio confirmar(UUID recepcionId,
            SolicitudDeConfirmacionDeRecepcion solicitud) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        Recepcion recepcion = delNegocio(recepcionId);
        if (solicitud != null) {
            recepcion.registrarFactura(solicitud.facturaProveedor());
        }
        recepcion.confirmar();

        List<RecepcionLinea> lineas = recepcionLineas.findByRecepcionId(recepcionId);

        if (recepcion.getOrdenId() != null) {
            aplicarSobreLaOrden(negocioId, recepcion.getOrdenId(), lineas);
        }
        abrirCuentaPorPagarSiHaceFalta(negocioId, recepcion);

        eventos.registrar(negocioId, "Recepcion", recepcionId, "recepcion_registrada",
                payload(negocioId, recepcion, lineas));

        return RecepcionDelNegocio.de(recepcion, lineas);
    }

    private void aplicarSobreLaOrden(UUID negocioId, UUID ordenId, List<RecepcionLinea> lineas) {
        Map<UUID, OrdenCompraLinea> lineasOrden = ordenLineas.findByOrdenIdOrderByLinea(ordenId)
                .stream().collect(Collectors.toMap(OrdenCompraLinea::getId, Function.identity()));

        for (RecepcionLinea l : lineas) {
            OrdenCompraLinea ol = lineasOrden.get(l.getOrdenLineaId());
            if (ol == null) {
                continue;
            }
            ol.recibir(l.getCantidad()); // criterio 4: 422 si pasa lo pedido + 5%
            ordenLineas.save(ol);
        }

        OrdenDeCompra orden = ordenes.findByIdAndNegocioId(ordenId, negocioId)
                .orElseThrow(() -> new NoEncontradoException("Esa orden no existe"));
        orden.registrarAvanceDeRecepcion(new ArrayList<>(lineasOrden.values())); // criterio 5
        ordenes.save(orden);
    }

    private void abrirCuentaPorPagarSiHaceFalta(UUID negocioId, Recepcion recepcion) {
        if (!recepcion.tieneFactura() || recepcion.getTotal().signum() <= 0) {
            return;
        }
        Proveedor proveedor = proveedores
                .findByIdAndNegocioId(recepcion.getProveedorId(), negocioId)
                .orElseThrow(() -> new NoEncontradoException("Ese proveedor no existe"));

        CuentaPorPagar cuenta = CuentaPorPagar.abrirPorRecepcion(negocioId,
                recepcion.getProveedorId(), recepcion.getId(), recepcion.getFacturaProveedor(),
                recepcion.getTotal().setScale(2, RoundingMode.HALF_UP), LocalDate.now(),
                proveedor.getDiasCredito());
        try {
            cuentas.save(cuenta);
            cuentas.flush();
        } catch (DataIntegrityViolationException duplicada) {
            throw new ConflictoDeEstadoException(
                    "Ya hay una cuenta por pagar con la factura " + recepcion.getFacturaProveedor());
        }
    }

    private Recepcion delNegocio(UUID recepcionId) {
        return recepciones.findByIdAndNegocioId(recepcionId, ContextoDeNegocio.negocioActual())
                .orElseThrow(() -> new NoEncontradoException("Esa recepción no existe"));
    }

    private static Map<String, Object> payload(UUID negocioId, Recepcion r,
            List<RecepcionLinea> lineas) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("negocio_id", negocioId.toString());
        datos.put("recepcion_id", r.getId().toString());
        datos.put("orden_id", r.getOrdenId() == null ? null : r.getOrdenId().toString());
        datos.put("proveedor_id", r.getProveedorId().toString());
        datos.put("bodega_id", r.getBodegaId().toString());
        datos.put("numero", r.getNumero());
        datos.put("factura_proveedor", r.getFacturaProveedor());
        List<Map<String, Object>> renglones = new ArrayList<>();
        for (RecepcionLinea l : lineas) {
            Map<String, Object> renglon = new LinkedHashMap<>();
            renglon.put("producto_id", l.getProductoId().toString());
            renglon.put("cantidad", l.getCantidad());
            renglon.put("costo_unitario", l.getCostoUnitario());
            renglon.put("codigo_lote", l.getCodigoLote());
            renglon.put("fecha_vencimiento",
                    l.getFechaVencimiento() == null ? null : l.getFechaVencimiento().toString());
            renglon.put("registro_sanitario", l.getRegistroSanitario());
            renglones.add(renglon);
        }
        datos.put("lineas", renglones);
        return datos;
    }
}
