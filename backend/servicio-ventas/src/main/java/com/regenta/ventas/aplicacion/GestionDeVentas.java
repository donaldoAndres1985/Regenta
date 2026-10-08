package com.regenta.ventas.aplicacion;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.comun.errores.NoEncontradoException;
import com.regenta.comun.errores.ReglaDeNegocioException;
import com.regenta.comun.negocio.ContextoDeNegocio;
import com.regenta.comun.negocio.RequierePermiso;
import com.regenta.ventas.domain.Venta;
import com.regenta.ventas.domain.VentaLinea;
import com.regenta.ventas.infra.AsignadorDeConsecutivos;
import com.regenta.ventas.infra.VentaLineaRepositorio;
import com.regenta.ventas.infra.VentaRepositorio;

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

    public GestionDeVentas(VentaRepositorio ventas, VentaLineaRepositorio lineas,
            AsignadorDeConsecutivos consecutivos, SagaDeConfirmacionDeVenta saga,
            ConfiguracionDelNegocio configuracion) {
        this.ventas = ventas;
        this.lineas = lineas;
        this.consecutivos = consecutivos;
        this.saga = saga;
        this.configuracion = configuracion;
    }

    @Transactional
    @RequierePermiso("VENTAS_VENTA_CREAR")
    public VentaDelNegocio crearBorrador(SolicitudDeVenta solicitud) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        long numero = consecutivos.siguiente(negocioId, "VENTA");
        Venta venta = Venta.crear(negocioId, solicitud.bodegaId(), Long.toString(numero),
                solicitud.clienteId(), ContextoDeNegocio.actual().usuario(),
                solicitud.listaPreciosId(), solicitud.canal());
        ventas.save(venta);
        return comoDto(venta);
    }

    @Transactional
    @RequierePermiso("VENTAS_VENTA_CREAR")
    public LineaDeVenta agregarLinea(UUID ventaId, SolicitudDeLinea solicitud) {
        Venta venta = ventaDelNegocio(ventaId);
        venta.exigirBorrador("agregar lineas");

        short numeroLinea = (short) (lineas.countByVentaId(ventaId) + 1);
        VentaLinea linea = VentaLinea.de(venta.getNegocioId(), ventaId, numeroLinea,
                solicitud.productoId(), solicitud.sku(), solicitud.nombre(), solicitud.unidad(),
                solicitud.cantidad(), solicitud.precioUnitario(), solicitud.descuentoPct(),
                solicitud.impuestoCodigo(), solicitud.impuestoPct(), solicitud.costoUnitario());
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
        exigirCompradorSiHaceFalta(ventaDelNegocio(ventaId));
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
        ventaDelNegocio(ventaId);   // valida negocio y existencia
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

    private void recalcular(Venta venta) {
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

    private VentaDelNegocio comoDto(Venta v) {
        List<LineaDeVenta> dto = lineas.findByVentaIdOrderByLinea(v.getId()).stream()
                .map(GestionDeVentas::comoDtoLinea)
                .toList();
        return new VentaDelNegocio(v.getId(), v.getNumero(), v.getEstado(), v.getSubtotal(),
                v.getDescuentoTotal(), v.getBaseGravable(), v.getImpuestoTotal(), v.getTotal(),
                v.getCostoTotal(), v.getClienteId(), v.getClienteSnapshot(), dto);
    }

    private static LineaDeVenta comoDtoLinea(VentaLinea l) {
        return new LineaDeVenta(l.getLinea(), l.getProductoId(), l.getSkuSnapshot(),
                l.getNombreSnapshot(), l.getCantidad(), l.getPrecioUnitario(), l.getDescuentoValor(),
                l.getImpuestoPct(), l.getImpuestoValor(), l.getBaseGravable(), l.getSubtotal(),
                l.getTotal());
    }
}
