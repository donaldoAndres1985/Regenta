package com.regenta.ventas.application;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.caja.api.CajaApi;
import com.regenta.shared.web.NoEncontradoException;
import com.regenta.shared.web.ReglaDeNegocioException;
import com.regenta.shared.tenancy.ContextoDeNegocio;
import com.regenta.shared.security.RequierePermiso;
import com.regenta.ventas.domain.Venta;
import com.regenta.ventas.domain.VentaLinea;
import com.regenta.ventas.infrastructure.AsignadorDeConsecutivos;
import com.regenta.ventas.infrastructure.VentaLineaRepositorio;
import com.regenta.ventas.infrastructure.VentaRepositorio;

/**
 * Armado de una venta en borrador. HU-037.
 *
 * <p>El número se asigna consecutivo por negocio al crear el borrador. Cada
 * línea guarda una copia (snapshot) del producto; editar el producto después no
 * la toca. Los totales se recalculan y se persisten en cada cambio de línea,
 * nunca se derivan al consultar. Fuera de {@code BORRADOR}, las líneas no se
 * editan (409).
 */
@Service
public class GestionDeVentas {

    private final VentaRepositorio ventas;
    private final VentaLineaRepositorio lineas;
    private final AsignadorDeConsecutivos consecutivos;
    private final SagaDeConfirmacionDeVenta saga;
    private final ConfiguracionDelNegocio configuracion;
    private final CajaApi caja;
    private final PrecioDeLinea precios;

    public GestionDeVentas(VentaRepositorio ventas, VentaLineaRepositorio lineas,
            AsignadorDeConsecutivos consecutivos, SagaDeConfirmacionDeVenta saga,
            ConfiguracionDelNegocio configuracion, CajaApi caja, PrecioDeLinea precios) {
        this.precios = precios;
        this.ventas = ventas;
        this.lineas = lineas;
        this.consecutivos = consecutivos;
        this.saga = saga;
        this.configuracion = configuracion;
        this.caja = caja;
    }

    @Transactional
    @RequierePermiso("VENTAS_VENTA_CREAR")
    public VentaDelNegocio crearBorrador(SolicitudDeVenta solicitud) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        long numero = consecutivos.siguiente(negocioId, "VENTA");
        // HU-132 · La lista la decide quien vende al asignar el cliente (y acepta o no el recálculo);
        // sin lista, los precios del negocio: el catálogo.
        Venta venta = Venta.crear(negocioId, solicitud.bodegaId(), Long.toString(numero),
                solicitud.clienteId(), ContextoDeNegocio.actual().usuario(),
                solicitud.listaPreciosId(), solicitud.canal());
        if (solicitud.notaCliente() != null && !solicitud.notaCliente().isBlank()) {
            venta.fijarNotaCliente(solicitud.notaCliente());
        }
        ventas.save(venta);
        return comoDto(venta);
    }

    /**
     * Las ventas en borrador que un cajero dejó a medio armar ("en espera"),
     * para que el POS las pueda recomponer en pestañas al volver a abrir.
     */
    @Transactional(readOnly = true)
    @RequierePermiso("VENTAS_VENTA_VER")
    public List<VentaDelNegocio> listarBorradoresAbiertos(UUID bodegaId) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        UUID usuarioId = ContextoDeNegocio.actual().usuario();
        return ventas
                .findByNegocioIdAndUsuarioIdAndBodegaIdAndEstadoOrderByCreadoEnDesc(negocioId,
                        usuarioId, bodegaId, com.regenta.ventas.domain.EstadoVenta.BORRADOR)
                .stream()
                .map(this::comoDto)
                .toList();
    }

    @Transactional
    @RequierePermiso("VENTAS_VENTA_CREAR")
    public LineaDeVenta agregarLinea(UUID ventaId, SolicitudDeLinea solicitud) {
        Venta venta = ventaDelNegocio(ventaId);
        if (solicitud.fraccion() != null && !solicitud.fraccion().isBlank()) {
            // HU-158 · I15: n fracciones descuentan n × factor y cobran n × precio de la fracción.
            PrecioDeLinea.Fraccionada f = precios.deFraccion(venta.getNegocioId(),
                    solicitud.productoId(), solicitud.fraccion(), solicitud.cantidad());
            SolicitudDeLinea comoUnidades = new SolicitudDeLinea(solicitud.productoId(),
                    solicitud.sku(), solicitud.nombre() + " (" + solicitud.fraccion() + ")",
                    solicitud.unidad(), f.cantidad(), solicitud.precioUnitario(),
                    solicitud.descuentoPct(), null, null, solicitud.costoUnitario(), null);
            return agregar(venta, comoUnidades, f.precio());
        }
        // HU-148 · I4: precio e IVA salen del catálogo; lo que mande el cliente se ignora.
        // HU-132: con lista de precios en la venta, el precio es el de la lista (si tiene el producto).
        PrecioDeLinea.Resuelto precio = venta.getListaPreciosId() == null
                ? precios.de(venta.getNegocioId(), solicitud.productoId())
                : precios.de(venta.getNegocioId(), solicitud.productoId(), venta.getListaPreciosId(),
                        solicitud.cantidad());
        return agregar(venta, solicitud, precio);
    }

    /**
     * Convertir una cotización respeta lo que se le cotizó al cliente: la línea copia el precio y
     * la tarifa que ya decidió el backend al cotizar, no el catálogo de hoy.
     */
    @Transactional
    @RequierePermiso("VENTAS_VENTA_CREAR")
    public LineaDeVenta agregarLineaCotizada(UUID ventaId, SolicitudDeLinea cotizada) {
        Venta venta = ventaDelNegocio(ventaId);
        return agregar(venta, cotizada, new PrecioDeLinea.Resuelto(cotizada.precioUnitario(),
                cotizada.impuestoCodigo(), cotizada.impuestoPct()));
    }

    private LineaDeVenta agregar(Venta venta, SolicitudDeLinea solicitud,
            PrecioDeLinea.Resuelto precio) {
        UUID ventaId = venta.getId();
        venta.exigirBorrador("agregar lineas");

        short numeroLinea = (short) (lineas.countByVentaId(ventaId) + 1);
        VentaLinea linea = VentaLinea.de(venta.getNegocioId(), ventaId, numeroLinea,
                solicitud.productoId(), solicitud.sku(), solicitud.nombre(), solicitud.unidad(),
                solicitud.cantidad(), precio.precio(), solicitud.descuentoPct(),
                precio.impuestoCodigo(), precio.impuestoPct(), solicitud.costoUnitario());
        lineas.save(linea);
        recalcular(venta);
        return comoDtoLinea(linea);
    }

    @Transactional
    @RequierePermiso("VENTAS_VENTA_CREAR")
    public void cambiarCantidad(UUID ventaId, short numeroLinea, BigDecimal cantidad) {
        Venta venta = ventaDelNegocio(ventaId);
        venta.exigirBorrador("editar lineas");
        VentaLinea linea = lineas.findByVentaIdAndLinea(ventaId, numeroLinea)
                .orElseThrow(() -> new NoEncontradoException("Esa linea no existe"));
        linea.cambiarCantidad(cantidad);
        lineas.save(linea);
        recalcular(venta);
    }

    /**
     * Confirma la venta: arranca la saga que reserva el stock en Inventario
     * (HU-038). La venta pasa a {@code PENDIENTE_STOCK}; se decide {@code CONFIRMADA}
     * o vuelve a {@code BORRADOR} cuando Inventario responde.
     */
    @Transactional
    @RequierePermiso("VENTAS_VENTA_CONFIRMAR")
    public void confirmar(UUID ventaId) {
        Venta venta = ventaDelNegocio(ventaId);
        exigirCompradorSiHaceFalta(venta);
        // HU-146: no se vende sin caja abierta; el dinero entra a la sesión de quien vende.
        UUID sesion = caja.sesionAbiertaDe(venta.getNegocioId(), ContextoDeNegocio.actual().usuario())
                .orElseThrow(() -> new ReglaDeNegocioException("Abre la caja antes de vender."));
        venta.asociarASesionDeCaja(sesion);
        ventas.save(venta);
        saga.iniciar(ventaId, SagaDeConfirmacionDeVenta.TIMEOUT_POR_DEFECTO);
    }

    /**
     * La venta que se cobró sin señal (HU-043) ya está cobrada: el dinero se
     * recibió en el mostrador. Al subir no se le vuelve a exigir el comprador
     * (HU-137) —rechazarla aquí no la descobra, solo la deja en conflicto—; la
     * app lo hace cumplir antes de cobrar con el monto que tiene guardado.
     */
    @Transactional
    @RequierePermiso("VENTAS_VENTA_CONFIRMAR")
    public void confirmarLaQueYaSeCobroSinSenal(UUID ventaId) {
        Venta venta = ventaDelNegocio(ventaId);   // valida negocio y existencia
        // HU-146 C3: ya se cobró; si hay caja abierta entra a ella, si no, no se rechaza.
        caja.sesionAbiertaDe(venta.getNegocioId(), ContextoDeNegocio.actual().usuario())
                .ifPresent(sesion -> {
                    venta.asociarASesionDeCaja(sesion);
                    ventas.save(venta);
                });
        saga.iniciar(ventaId, SagaDeConfirmacionDeVenta.TIMEOUT_POR_DEFECTO);
    }

    /**
     * HU-137 criterio 2: sobre el monto que configuró el negocio, la venta no
     * se cobra a consumidor final. Sin monto configurado no se exige nada
     * (criterio 3).
     */
    private void exigirCompradorSiHaceFalta(Venta venta) {
        if (venta.getClienteId() != null) {
            return;
        }
        configuracion.montoParaIdentificarComprador(venta.getNegocioId())
                .filter(monto -> venta.getTotal().compareTo(monto) > 0)
                .ifPresent(monto -> {
                    throw new ReglaDeNegocioException("La venta supera " + monto.toPlainString()
                            + ": hay que identificar al comprador antes de cobrar");
                });
    }

    @Transactional(readOnly = true)
    @RequierePermiso("VENTAS_VENTA_VER")
    public VentaDelNegocio ver(UUID ventaId) {
        return comoDto(ventaDelNegocio(ventaId));
    }

    /** Totales de la venta desde sus líneas. HU-132: también lo usa el recálculo por cliente. */
    void recalcular(Venta venta) {
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal descuento = BigDecimal.ZERO;
        BigDecimal base = BigDecimal.ZERO;
        BigDecimal impuesto = BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal costo = BigDecimal.ZERO;
        for (VentaLinea l : lineas.findByVentaIdOrderByLinea(venta.getId())) {
            subtotal = subtotal.add(l.getSubtotal());
            descuento = descuento.add(l.getDescuentoValor());
            base = base.add(l.getBaseGravable());
            impuesto = impuesto.add(l.getImpuestoValor());
            total = total.add(l.getTotal());
            costo = costo.add(l.getCostoUnitarioSnapshot().multiply(l.getCantidad()));
        }
        venta.fijarTotales(subtotal, descuento, base, impuesto, total, costo);
        ventas.save(venta);
    }

    private Venta ventaDelNegocio(UUID ventaId) {
        Venta venta = ventas.findById(ventaId)
                .orElseThrow(() -> new NoEncontradoException("Esa venta no existe"));
        if (!venta.getNegocioId().equals(ContextoDeNegocio.negocioActual())) {
            throw new NoEncontradoException("Esa venta no existe");
        }
        return venta;
    }

    @Transactional(readOnly = true)
    @RequierePermiso("VENTAS_VENTA_VER")
    public List<VentaDelNegocio> buscarVentas(String q) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        List<String> tokens = (q == null || q.isBlank())
                ? List.of()
                : Arrays.stream(q.trim().split("\\s+")).filter(s -> !s.isBlank()).toList();
        return ventas.buscarPorTokens(negocioId, tokens).stream()
                .map(this::comoDto)
                .toList();
    }

    private VentaDelNegocio comoDto(Venta v) {
        List<VentaLinea> deLaVenta = lineas.findByVentaIdOrderByLinea(v.getId());
        List<LineaDeVenta> dto = deLaVenta.stream()
                .map(GestionDeVentas::comoDtoLinea)
                .toList();
        return new VentaDelNegocio(v.getId(), v.getNumero(), v.getEstado(), v.getSubtotal(),
                v.getDescuentoTotal(), v.getBaseGravable(), v.getImpuestoTotal(), v.getTotal(),
                v.getCostoTotal(), v.getClienteId(), v.getClienteSnapshot(), v.getNotaCliente(),
                v.estaFacturada(), dto, porTarifa(deLaVenta));
    }

    /** HU-148 C6: base e IVA agrupados por tarifa, en el orden en que aparecen en la venta. */
    private static List<ImpuestoPorTarifa> porTarifa(List<VentaLinea> deLaVenta) {
        java.util.Map<String, ImpuestoPorTarifa> grupos = new java.util.LinkedHashMap<>();
        for (VentaLinea l : deLaVenta) {
            String codigo = l.getImpuestoCodigo() == null ? "SIN_TARIFA" : l.getImpuestoCodigo();
            grupos.merge(codigo,
                    new ImpuestoPorTarifa(codigo, l.getImpuestoPct(), l.getBaseGravable(),
                            l.getImpuestoValor()),
                    (a, b) -> new ImpuestoPorTarifa(codigo, a.porcentaje(), a.base().add(b.base()),
                            a.iva().add(b.iva())));
        }
        return List.copyOf(grupos.values());
    }

    private static LineaDeVenta comoDtoLinea(VentaLinea l) {
        return new LineaDeVenta(l.getLinea(), l.getProductoId(), l.getSkuSnapshot(),
                l.getNombreSnapshot(), l.getCantidad(), l.getPrecioUnitario(), l.getDescuentoValor(),
                l.getImpuestoPct(), l.getImpuestoCodigo(), l.getImpuestoValor(), l.getBaseGravable(),
                l.getSubtotal(), l.getTotal());
    }
}
