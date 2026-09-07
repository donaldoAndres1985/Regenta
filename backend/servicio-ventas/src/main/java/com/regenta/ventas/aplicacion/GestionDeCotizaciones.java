package com.regenta.ventas.aplicacion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.comun.errores.ConflictoDeEstadoException;
import com.regenta.comun.errores.NoEncontradoException;
import com.regenta.comun.negocio.ContextoDeNegocio;
import com.regenta.comun.negocio.RequierePermiso;
import com.regenta.ventas.domain.Cotizacion;
import com.regenta.ventas.domain.CotizacionLinea;
import com.regenta.ventas.domain.EstadoCotizacion;
import com.regenta.ventas.infra.AsignadorDeConsecutivos;
import com.regenta.ventas.infra.CotizacionLineaRepositorio;
import com.regenta.ventas.infra.CotizacionRepositorio;

/**
 * Cotizaciones que se convierten en venta. HU-044.
 *
 * <p>Una cotización guarda sus líneas con el mismo snapshot que la venta.
 * {@code convertir} crea una venta en borrador con esas mismas líneas y deja la
 * cotización enlazada a ella (criterios 1 y 3). Si la cotización ya venció, la
 * conversión avisa que los precios pueden haber cambiado, pero se hace igual
 * (criterio 2).
 */
@Service
public class GestionDeCotizaciones {

    private final CotizacionRepositorio cotizaciones;
    private final CotizacionLineaRepositorio lineas;
    private final AsignadorDeConsecutivos consecutivos;
    private final GestionDeVentas ventas;

    public GestionDeCotizaciones(CotizacionRepositorio cotizaciones,
            CotizacionLineaRepositorio lineas, AsignadorDeConsecutivos consecutivos,
            GestionDeVentas ventas) {
        this.cotizaciones = cotizaciones;
        this.lineas = lineas;
        this.consecutivos = consecutivos;
        this.ventas = ventas;
    }

    @Transactional
    @RequierePermiso("VENTAS_COTIZACION_CREAR")
    public CotizacionDelNegocio crear(SolicitudDeCotizacion solicitud) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        long numero = consecutivos.siguiente(negocioId, "COTIZACION");
        Cotizacion cotizacion = Cotizacion.crear(negocioId, Long.toString(numero),
                solicitud.clienteId(), ContextoDeNegocio.actual().usuario(),
                solicitud.validaHasta());
        cotizaciones.save(cotizacion);

        BigDecimal total = BigDecimal.ZERO;
        short numeroLinea = 1;
        for (LineaDeCotizacion l : solicitud.lineas()) {
            CotizacionLinea linea = CotizacionLinea.de(negocioId, cotizacion.getId(), numeroLinea++,
                    l.productoId(), l.sku(), l.nombre(), l.unidad(), l.cantidad(),
                    l.precioUnitario(), l.descuentoPct(), l.impuestoCodigo(), l.impuestoPct(),
                    l.costoUnitario());
            lineas.save(linea);
            total = total.add(linea.getTotal());
        }
        cotizacion.fijarTotal(total);
        cotizaciones.save(cotizacion);
        return comoDto(cotizacion);
    }

    @Transactional
    @RequierePermiso("VENTAS_COTIZACION_CONVERTIR")
    public ResultadoDeConversion convertir(UUID cotizacionId, UUID bodegaId) {
        Cotizacion cotizacion = cotizacionDelNegocio(cotizacionId);
        if (cotizacion.getEstado() == EstadoCotizacion.CONVERTIDA) {
            throw new ConflictoDeEstadoException("La cotizacion " + cotizacion.getNumero()
                    + " ya se convirtio en la venta " + cotizacion.getVentaId());
        }

        VentaDelNegocio venta = ventas.crearBorrador(new SolicitudDeVenta(bodegaId,
                cotizacion.getClienteId(), null, "MOSTRADOR"));
        for (CotizacionLinea cl : lineas.findByCotizacionIdOrderByLinea(cotizacionId)) {
            ventas.agregarLinea(venta.id(), new SolicitudDeLinea(cl.getProductoId(),
                    cl.getSkuSnapshot(), cl.getNombreSnapshot(), cl.getUnidadSnapshot(),
                    cl.getCantidad(), cl.getPrecioUnitario(), cl.getDescuentoPct(),
                    cl.getImpuestoCodigo(), cl.getImpuestoPct(), cl.getCostoUnitarioSnapshot()));
        }

        boolean preciosDesactualizados = cotizacion.estaVencida(LocalDate.now());
        cotizacion.marcarConvertida(venta.id());
        cotizaciones.save(cotizacion);
        return new ResultadoDeConversion(venta.id(), venta.numero(), preciosDesactualizados);
    }

    @Transactional(readOnly = true)
    @RequierePermiso("VENTAS_COTIZACION_VER")
    public CotizacionDelNegocio ver(UUID cotizacionId) {
        return comoDto(cotizacionDelNegocio(cotizacionId));
    }

    private Cotizacion cotizacionDelNegocio(UUID cotizacionId) {
        Cotizacion cotizacion = cotizaciones.findById(cotizacionId)
                .orElseThrow(() -> new NoEncontradoException("Esa cotizacion no existe"));
        if (!cotizacion.getNegocioId().equals(ContextoDeNegocio.negocioActual())) {
            throw new NoEncontradoException("Esa cotizacion no existe");
        }
        return cotizacion;
    }

    private static CotizacionDelNegocio comoDto(Cotizacion c) {
        return new CotizacionDelNegocio(c.getId(), c.getNumero(), c.getEstado(), c.getValidaHasta(),
                c.getTotal(), c.getVentaId());
    }
}
