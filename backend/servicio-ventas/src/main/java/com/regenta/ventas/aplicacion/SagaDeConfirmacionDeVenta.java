package com.regenta.ventas.aplicacion;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
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
import com.regenta.ventas.domain.EstadoVenta;
import com.regenta.ventas.domain.Saga;
import com.regenta.ventas.domain.Venta;
import com.regenta.ventas.domain.VentaLinea;
import com.regenta.ventas.infra.SagaRepositorio;
import com.regenta.ventas.infra.VentaLineaRepositorio;
import com.regenta.ventas.infra.VentaRepositorio;

/**
 * La saga que reemplaza al {@code @Transactional} que no existe entre servicios.
 * HU-038.
 *
 * <p>Confirmar una venta la pasa a {@code PENDIENTE_STOCK} y publica
 * {@code solicitar_reserva_stock} en la misma transacción (criterio 1). Cuando
 * Inventario responde, {@code stock_reservado} confirma la venta y publica
 * {@code venta_completada} (criterio 2); {@code stock_reserva_fallida} —o el
 * vencimiento del timeout— la devuelve a {@code BORRADOR} y deja la saga
 * {@code COMPENSADA} (criterios 3 y 4). El estado vive en la tabla {@code sagas}:
 * una instancia nueva retoma desde ahí (criterio 5).
 */
@Service
public class SagaDeConfirmacionDeVenta {

    /** Cuánto espera la saga por la respuesta de Inventario antes de compensar. */
    public static final Duration TIMEOUT_POR_DEFECTO = Duration.ofMinutes(5);

    private final VentaRepositorio ventas;
    private final VentaLineaRepositorio lineas;
    private final SagaRepositorio sagas;
    private final RegistroDeEventos eventos;

    public SagaDeConfirmacionDeVenta(VentaRepositorio ventas, VentaLineaRepositorio lineas,
            SagaRepositorio sagas, RegistroDeEventos eventos) {
        this.ventas = ventas;
        this.lineas = lineas;
        this.sagas = sagas;
        this.eventos = eventos;
    }

    /** Criterio 1. */
    @Transactional
    public void iniciar(UUID ventaId, Duration timeout) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        Venta venta = ventaDelNegocio(ventaId);
        venta.aPendienteStock();
        ventas.save(venta);

        Saga saga = Saga.paraVenta(negocioId, ventaId,
                OffsetDateTime.now().plus(timeout == null ? TIMEOUT_POR_DEFECTO : timeout));
        Map<String, Object> payload = payloadSolicitud(negocioId, venta, saga);
        saga.guardarPayload(payload);
        sagas.save(saga);

        eventos.registrar(negocioId, "Saga", saga.getId(), "solicitar_reserva_stock", payload);
    }

    /** Criterio 2. */
    @Transactional
    public void alStockReservado(UUID correlacionId) {
        Saga saga = sagaPorCorrelacion(correlacionId);
        if (!saga.estaEsperando()) {
            return;   // evento repetido: idempotente
        }
        saga.completar();
        Venta venta = ventaDelNegocio(saga.getAgregadoId());
        venta.aConfirmada();
        ventas.save(venta);

        eventos.registrar(saga.getNegocioId(), "Venta", venta.getId(), "venta_completada",
                payloadCompletada(venta, lineas.findByVentaIdOrderByLinea(venta.getId())));
    }

    /** Criterio 3. */
    @Transactional
    public void alReservaFallida(UUID correlacionId, String motivo) {
        Saga saga = sagaPorCorrelacion(correlacionId);
        if (!saga.estaEsperando()) {
            return;
        }
        compensarSaga(saga, motivo == null || motivo.isBlank() ? "no hay stock suficiente" : motivo);
    }

    /** Criterio 4: el barrido compensa las sagas que vencieron sin respuesta. */
    @Transactional
    public int compensarVencidas() {
        List<Saga> vencidas = sagas.tomarVencidas(OffsetDateTime.now());
        for (Saga saga : vencidas) {
            compensarSaga(saga, "timeout de reserva de stock");
        }
        return vencidas.size();
    }

    private void compensarSaga(Saga saga, String motivo) {
        saga.compensar(motivo);
        ventas.findById(saga.getAgregadoId()).ifPresent(venta -> {
            if (venta.getEstado() == EstadoVenta.PENDIENTE_STOCK) {
                venta.volverABorrador(motivo);
                ventas.save(venta);
                avisarSiVinoDeLaColaOffline(saga.getNegocioId(), venta, motivo);
            }
        });
    }

    /**
     * HU-043 criterio 4: la venta se registró sin señal y, para cuando subió,
     * el stock ya no alcanzaba. Queda en BORRADOR —no se pierde, el vendedor
     * decide qué hacer— y se avisa: nadie está mirando la pantalla en el
     * momento en que esto pasa, así que enterarse no puede depender de que
     * alguien revise la lista de ventas.
     */
    private void avisarSiVinoDeLaColaOffline(UUID negocioId, Venta venta, String motivo) {
        if (!venta.vinoDeLaColaOffline()) {
            return;
        }
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("negocio_id", negocioId.toString());
        datos.put("venta_id", venta.getId().toString());
        datos.put("numero", venta.getNumero());
        datos.put("origen_offline_id", venta.getOrigenOfflineId().toString());
        datos.put("dispositivo_id", venta.getDispositivoId());
        datos.put("usuario_id", venta.getUsuarioId() == null ? null : venta.getUsuarioId().toString());
        datos.put("motivo", motivo);
        eventos.registrar(negocioId, "Venta", venta.getId(), "venta_offline_en_conflicto", datos);
    }

    private Map<String, Object> payloadSolicitud(UUID negocioId, Venta venta, Saga saga) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("negocio_id", negocioId.toString());
        datos.put("origen_tipo", "VENTA");
        datos.put("origen_id", venta.getId().toString());
        datos.put("correlacion_id", saga.getCorrelacionId().toString());
        List<Map<String, Object>> renglones = new ArrayList<>();
        for (VentaLinea l : lineas.findByVentaIdOrderByLinea(venta.getId())) {
            Map<String, Object> renglon = new LinkedHashMap<>();
            renglon.put("producto_id", l.getProductoId().toString());
            renglon.put("bodega_id", venta.getBodegaId().toString());
            renglon.put("cantidad", l.getCantidad());
            renglones.add(renglon);
        }
        datos.put("lineas", renglones);
        return datos;
    }

    /**
     * El detalle de líneas viaja en el evento a propósito (HU-096 criterio 1):
     * {@code hechos_venta} tiene grano de línea, y Reportes no puede consultar
     * la base de Ventas para completarlo.
     */
    private static Map<String, Object> payloadCompletada(Venta venta, List<VentaLinea> lineas) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("negocio_id", venta.getNegocioId().toString());
        datos.put("venta_id", venta.getId().toString());
        datos.put("numero", venta.getNumero());
        datos.put("bodega_id", venta.getBodegaId().toString());
        datos.put("cliente_id", venta.getClienteId() == null ? null : venta.getClienteId().toString());
        datos.put("usuario_id", venta.getUsuarioId().toString());
        datos.put("canal", venta.getCanal());
        datos.put("fecha", venta.getFecha().toString());
        datos.put("total", venta.getTotal());
        List<Map<String, Object>> lineasPayload = new ArrayList<>();
        for (VentaLinea l : lineas) {
            Map<String, Object> lp = new LinkedHashMap<>();
            lp.put("producto_id", l.getProductoId().toString());
            lp.put("sku", l.getSkuSnapshot());
            lp.put("nombre", l.getNombreSnapshot());
            lp.put("cantidad", l.getCantidad());
            lp.put("precio_unitario", l.getPrecioUnitario());
            lp.put("descuento_valor", l.getDescuentoValor());
            lp.put("impuesto_valor", l.getImpuestoValor());
            lp.put("total", l.getTotal());
            lp.put("costo_unitario", l.getCostoUnitarioSnapshot());
            lineasPayload.add(lp);
        }
        datos.put("lineas", lineasPayload);
        return datos;
    }

    private Saga sagaPorCorrelacion(UUID correlacionId) {
        return sagas.findByCorrelacionId(correlacionId)
                .orElseThrow(() -> new NoEncontradoException(
                        "No hay saga con la correlacion " + correlacionId));
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
