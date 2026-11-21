package com.regenta.compras.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.compras.domain.CuentaPorPagar;
import com.regenta.compras.domain.DevolucionAProveedor;
import com.regenta.compras.domain.LineaDeDevolucionAProveedor;
import com.regenta.compras.domain.EstadoCuentaPorPagar;
import com.regenta.compras.domain.NotaCreditoProveedor;
import com.regenta.compras.domain.Recepcion;
import com.regenta.compras.domain.RecepcionLinea;
import com.regenta.compras.infrastructure.AsignadorDeConsecutivos;
import com.regenta.compras.infrastructure.CuentaPorPagarRepositorio;
import com.regenta.compras.infrastructure.LineaDeDevolucionAProveedorRepositorio;
import com.regenta.compras.infrastructure.DevolucionAProveedorRepositorio;
import com.regenta.compras.infrastructure.NotaCreditoRepositorio;
import com.regenta.compras.infrastructure.RecepcionLineaRepositorio;
import com.regenta.compras.infrastructure.RecepcionRepositorio;
import com.regenta.inventario.api.InventarioApi;
import com.regenta.inventario.api.SalidaPrevista;
import com.regenta.shared.events.RegistroDeEventos;
import com.regenta.shared.security.RequierePermiso;
import com.regenta.shared.tenancy.ContextoDeNegocio;
import com.regenta.shared.web.ConflictoDeEstadoException;
import com.regenta.shared.web.NoEncontradoException;
import com.regenta.shared.web.ReglaDeNegocioException;

/**
 * HU-178 · Devolverle al proveedor mercancía de una recepción confirmada (I27): sale de la bodega del
 * lote de origen al costo de entrada ({@code devolucion_proveedor_registrada}), y nace una nota
 * crédito por el valor facturado de lo devuelto — base con sus descuentos (también la parte del
 * descuento global) más su IVA; el flete no se devuelve. La nota baja el saldo de la cuenta de esa
 * recepción y lo que sobra queda a favor del negocio con el proveedor.
 */
@Service
public class GestionDeDevolucionesAProveedor {

    private static final String TIPO_CONSECUTIVO = "DEVOLUCION_PROVEEDOR";
    private static final String EVENTO = "devolucion_proveedor_registrada";

    private final DevolucionAProveedorRepositorio devoluciones;
    private final LineaDeDevolucionAProveedorRepositorio lineasDevueltas;
    private final NotaCreditoRepositorio notas;
    private final RecepcionRepositorio recepciones;
    private final RecepcionLineaRepositorio recepcionLineas;
    private final CuentaPorPagarRepositorio cuentas;
    private final AsignadorDeConsecutivos consecutivos;
    private final InventarioApi inventario;
    private final RegistroDeEventos eventos;

    public GestionDeDevolucionesAProveedor(DevolucionAProveedorRepositorio devoluciones, LineaDeDevolucionAProveedorRepositorio lineasDevueltas,
            NotaCreditoRepositorio notas, RecepcionRepositorio recepciones, RecepcionLineaRepositorio recepcionLineas,
            CuentaPorPagarRepositorio cuentas, AsignadorDeConsecutivos consecutivos, InventarioApi inventario,
            RegistroDeEventos eventos) {
        this.devoluciones = devoluciones;
        this.lineasDevueltas = lineasDevueltas;
        this.notas = notas;
        this.recepciones = recepciones;
        this.recepcionLineas = recepcionLineas;
        this.cuentas = cuentas;
        this.consecutivos = consecutivos;
        this.inventario = inventario;
        this.eventos = eventos;
    }

    @Transactional
    @RequierePermiso("COMPRAS_DEVOLUCION_REGISTRAR")
    public DevolucionAProveedorDelNegocio registrar(SolicitudDeDevolucionAProveedor solicitud) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        Recepcion recepcion = recepciones.findByIdAndNegocioId(solicitud.recepcionId(), negocioId)
                .orElseThrow(() -> new NoEncontradoException("Esa recepción no existe"));
        if (recepcion.getEstado() != com.regenta.compras.domain.EstadoRecepcion.CONFIRMADA) {
            throw new ConflictoDeEstadoException(
                    "Se devuelve mercancía de una recepción confirmada; esta está " + recepcion.getEstado());
        }

        List<RecepcionLinea> suyas = recepcionLineas.findByRecepcionId(recepcion.getId());
        Map<UUID, RecepcionLinea> porId = suyas.stream()
                .collect(Collectors.toMap(RecepcionLinea::getId, Function.identity()));
        Map<UUID, BigDecimal> yaDevuelto = new LinkedHashMap<>();
        for (LineaDeDevolucionAProveedor d : lineasDevueltas.findByRecepcionLineaIdIn(porId.keySet())) {
            yaDevuelto.merge(d.getRecepcionLineaId(), d.getCantidad(), BigDecimal::add);
        }

        // C4 · Nunca más de lo recibido menos lo ya devuelto (las líneas repetidas suman).
        Map<UUID, BigDecimal> pedido = new LinkedHashMap<>();
        for (SolicitudDeDevolucionAProveedor.Linea l : solicitud.lineas()) {
            RecepcionLinea origen = porId.get(l.recepcionLineaId());
            if (origen == null) {
                throw new NoEncontradoException("La línea " + l.recepcionLineaId() + " no es de esa recepción");
            }
            BigDecimal total = pedido.merge(origen.getId(), l.cantidad(), BigDecimal::add);
            BigDecimal devuelto = yaDevuelto.getOrDefault(origen.getId(), BigDecimal.ZERO);
            BigDecimal quedan = origen.getCantidad().subtract(devuelto);
            if (total.compareTo(quedan) > 0) {
                throw new ReglaDeNegocioException("De «" + origen.getNombre() + "» se recibieron "
                        + plano(origen.getCantidad()) + " y ya se devolvieron " + plano(devuelto)
                        + ": se pueden devolver " + plano(quedan.max(BigDecimal.ZERO)));
            }
        }

        // C5 / C6 · Tiene que estar en la bodega, y en el lote de origen (409, sin cambiar nada).
        List<String> faltan = inventario.faltantesParaSacar(negocioId, recepcion.getBodegaId(), pedido.entrySet()
                .stream()
                .map(e -> new SalidaPrevista(porId.get(e.getKey()).getProductoId(), e.getValue(),
                        porId.get(e.getKey()).getCodigoLote()))
                .toList());
        if (!faltan.isEmpty()) {
            throw new ConflictoDeEstadoException("No hay en la bodega lo que se quiere devolver: "
                    + String.join("; ", faltan));
        }

        BigDecimal factorGlobal = factorDeDescuentoGlobal(recepcion, suyas);
        String numero = "DEV-" + consecutivos.siguiente(negocioId, TIPO_CONSECUTIVO);
        List<LineaDeDevolucionAProveedor> lineas = new ArrayList<>();
        BigDecimal valor = BigDecimal.ZERO;
        // Primero los valores: la devolución nace con el de su nota.
        List<Object[]> calculadas = new ArrayList<>();
        for (Map.Entry<UUID, BigDecimal> e : pedido.entrySet()) {
            RecepcionLinea origen = porId.get(e.getKey());
            BigDecimal proporcion = e.getValue().divide(origen.getCantidad(), 10, RoundingMode.HALF_UP);
            BigDecimal base = origen.subtotal().multiply(factorGlobal).multiply(proporcion)
                    .setScale(2, RoundingMode.HALF_UP);
            BigDecimal iva = origen.impuesto().multiply(proporcion).setScale(2, RoundingMode.HALF_UP);
            valor = valor.add(base).add(iva);
            calculadas.add(new Object[] {origen, e.getValue(), base, iva});
        }

        DevolucionAProveedor devolucion = DevolucionAProveedor.registrar(negocioId, numero, recepcion,
                solicitud.motivo(), valor, ContextoDeNegocio.actual().usuario());
        for (Object[] c : calculadas) {
            lineas.add(LineaDeDevolucionAProveedor.de(negocioId, devolucion.getId(), (RecepcionLinea) c[0], (BigDecimal) c[1],
                    (BigDecimal) c[2], (BigDecimal) c[3]));
        }

        // C2 / C3 · La nota baja la cuenta de esa recepción; lo que no cabe queda a favor.
        CuentaPorPagar cuenta = cuentas.findByNegocioIdAndRecepcionId(negocioId, recepcion.getId()).stream()
                .filter(c -> c.getEstado() != EstadoCuentaPorPagar.ANULADA)
                .findFirst().orElse(null);
        NotaCreditoProveedor nota = NotaCreditoProveedor.emitir(negocioId, devolucion, cuenta, valor);

        recepcion.registrarDevolucion();
        devoluciones.save(devolucion);
        lineasDevueltas.saveAll(lineas);
        notas.save(nota);
        if (cuenta != null) {
            cuentas.save(cuenta);
        }
        recepciones.save(recepcion);
        eventos.registrar(negocioId, "DevolucionAProveedor", devolucion.getId(), EVENTO,
                payload(negocioId, devolucion, nota, lineas));
        return DevolucionAProveedorDelNegocio.de(devolucion, lineas, nota);
    }

    @Transactional(readOnly = true)
    @RequierePermiso("COMPRAS_COMPRA_VER")
    public List<DevolucionAProveedorDelNegocio> listar(UUID recepcionId) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        List<DevolucionAProveedor> filas = recepcionId == null
                ? devoluciones.findByNegocioIdOrderByFechaDesc(negocioId)
                : devoluciones.findByNegocioIdAndRecepcionIdOrderByFechaDesc(negocioId, recepcionId);
        return filas.stream().map(this::vista).toList();
    }

    @Transactional(readOnly = true)
    @RequierePermiso("COMPRAS_COMPRA_VER")
    public DevolucionAProveedorDelNegocio ver(UUID devolucionId) {
        return vista(devoluciones.findByIdAndNegocioId(devolucionId, ContextoDeNegocio.negocioActual())
                .orElseThrow(() -> new NoEncontradoException("Esa devolución no existe")));
    }

    private DevolucionAProveedorDelNegocio vista(DevolucionAProveedor d) {
        return DevolucionAProveedorDelNegocio.de(d, lineasDevueltas.findByDevolucionId(d.getId()),
                notas.findByDevolucionId(d.getId()).orElse(null));
    }

    /** La parte de la base que quedó después del descuento global de la factura (HU-168). */
    private static BigDecimal factorDeDescuentoGlobal(Recepcion recepcion, List<RecepcionLinea> lineas) {
        BigDecimal descuento = recepcion.getDescuentoGlobal();
        if (descuento == null || descuento.signum() <= 0) {
            return BigDecimal.ONE;
        }
        BigDecimal bruto = lineas.stream().map(RecepcionLinea::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (bruto.signum() <= 0) {
            return BigDecimal.ONE;
        }
        return BigDecimal.ONE.subtract(descuento.divide(bruto, 10, RoundingMode.HALF_UP)).max(BigDecimal.ZERO);
    }

    /** Con la forma de {@code recepcion_registrada}, para que Inventario lo lea igual. */
    private static Map<String, Object> payload(UUID negocioId, DevolucionAProveedor d, NotaCreditoProveedor nota,
            List<LineaDeDevolucionAProveedor> lineas) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("negocio_id", negocioId.toString());
        datos.put("devolucion_id", d.getId().toString());
        datos.put("recepcion_id", d.getRecepcionId().toString());
        datos.put("proveedor_id", d.getProveedorId().toString());
        datos.put("bodega_id", d.getBodegaId().toString());
        datos.put("numero", d.getNumero());
        datos.put("motivo", d.getMotivo());
        datos.put("valor_nota", d.getValorNota());
        datos.put("aplicado_a_cuenta", nota.getAplicadoACuenta());
        datos.put("saldo_a_favor", nota.getExcedente());
        UUID usuario = ContextoDeNegocio.actual().usuario();
        datos.put("usuario_id", usuario == null ? null : usuario.toString());
        List<Map<String, Object>> renglones = new ArrayList<>();
        for (LineaDeDevolucionAProveedor l : lineas) {
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("producto_id", l.getProductoId().toString());
            r.put("cantidad", l.getCantidad());
            r.put("costo_unitario", l.getCostoEntrada());
            r.put("codigo_lote", l.getCodigoLote());
            renglones.add(r);
        }
        datos.put("lineas", renglones);
        return datos;
    }

    private static String plano(BigDecimal v) {
        return v.stripTrailingZeros().toPlainString();
    }
}
