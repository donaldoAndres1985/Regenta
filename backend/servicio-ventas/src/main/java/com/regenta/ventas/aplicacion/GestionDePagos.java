package com.regenta.ventas.aplicacion;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.comun.errores.NoEncontradoException;
import com.regenta.comun.errores.ReglaDeNegocioException;
import com.regenta.comun.eventos.RegistroDeEventos;
import com.regenta.comun.negocio.ContextoDeNegocio;
import com.regenta.comun.negocio.RequierePermiso;
import com.regenta.ventas.domain.MetodoDePago;
import com.regenta.ventas.domain.PagoDeVenta;
import com.regenta.ventas.domain.Venta;
import com.regenta.ventas.infra.PagoDeVentaRepositorio;
import com.regenta.ventas.infra.VentaRepositorio;

/**
 * El cobro de una venta. HU-039.
 *
 * <p>Se pueden registrar varios pagos (mixto). El efectivo mayor al saldo
 * genera cambio (criterio 2); un pago a crédito exige cliente, fija el
 * vencimiento y publica {@code venta_a_credito} (criterio 4); la tarjeta guarda
 * su voucher (criterio 5). Una venta con pagos por menos del total no se cierra
 * (criterio 1).
 */
@Service
public class GestionDePagos {

    private static final int ESCALA = 4;
    private static final int DIAS_CREDITO_POR_DEFECTO = 30;

    private final VentaRepositorio ventas;
    private final PagoDeVentaRepositorio pagos;
    private final RegistroDeEventos eventos;

    public GestionDePagos(VentaRepositorio ventas, PagoDeVentaRepositorio pagos,
            RegistroDeEventos eventos) {
        this.ventas = ventas;
        this.pagos = pagos;
        this.eventos = eventos;
    }

    @Transactional
    @RequierePermiso("VENTAS_PAGO_REGISTRAR")
    public ResultadoDePago registrarPago(UUID ventaId, SolicitudDePago solicitud) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        Venta venta = ventaDelNegocio(ventaId);
        venta.exigirConfirmada("cobrar");

        BigDecimal saldo = venta.getSaldoPendiente();
        if (saldo.signum() <= 0) {
            throw new ReglaDeNegocioException("La venta " + venta.getNumero() + " ya esta cubierta");
        }

        BigDecimal recibido = null;
        BigDecimal aplicado;
        BigDecimal cambio = BigDecimal.ZERO;
        if (solicitud.metodo() == MetodoDePago.EFECTIVO) {
            recibido = solicitud.montoRecibido() != null ? solicitud.montoRecibido()
                    : solicitud.monto();
            if (recibido == null) {
                throw new ReglaDeNegocioException("Indica cuanto se recibio en efectivo");
            }
            aplicado = recibido.min(saldo);
            cambio = recibido.subtract(aplicado);
        } else {
            aplicado = solicitud.monto();
            if (aplicado == null) {
                throw new ReglaDeNegocioException("Indica el monto del pago");
            }
            if (aplicado.compareTo(saldo) > 0) {
                throw new ReglaDeNegocioException("El pago de " + escala(aplicado)
                        + " supera el saldo pendiente de " + escala(saldo));
            }
        }
        aplicado = escala(aplicado);
        cambio = escala(cambio);

        LocalDate vencimiento = null;
        if (solicitud.metodo() == MetodoDePago.CREDITO) {
            if (venta.getClienteId() == null) {
                throw new ReglaDeNegocioException(
                        "Una venta a credito necesita un cliente asignado");
            }
            int dias = solicitud.diasCredito() != null && solicitud.diasCredito() > 0
                    ? solicitud.diasCredito() : DIAS_CREDITO_POR_DEFECTO;
            vencimiento = LocalDate.now().plusDays(dias);
        }

        PagoDeVenta pago = PagoDeVenta.de(negocioId, ventaId, solicitud.metodo(), aplicado, recibido,
                cambio, solicitud.referencia(), solicitud.franquicia());
        pagos.save(pago);

        BigDecimal cubierto = pagos.sumaAplicadaDe(ventaId);
        BigDecimal nuevoSaldo = escala(venta.getTotal().subtract(cubierto)).max(BigDecimal.ZERO);
        venta.registrarCobro(nuevoSaldo, formaPagoDe(ventaId), vencimiento);
        ventas.save(venta);

        if (solicitud.metodo() == MetodoDePago.CREDITO) {
            eventos.registrar(negocioId, "Venta", ventaId, "venta_a_credito",
                    payloadCredito(negocioId, venta, aplicado, vencimiento));
        }
        return new ResultadoDePago(pago.getId(), solicitud.metodo(), aplicado, cambio, nuevoSaldo,
                nuevoSaldo.signum() <= 0);
    }

    /** Criterio 1: cerrar exige que los pagos cubran el total. */
    @Transactional
    @RequierePermiso("VENTAS_PAGO_REGISTRAR")
    public void cerrarVenta(UUID ventaId) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        Venta venta = ventaDelNegocio(ventaId);
        venta.exigirConfirmada("cerrar");

        BigDecimal falta = escala(venta.getTotal().subtract(pagos.sumaAplicadaDe(ventaId)));
        if (falta.signum() > 0) {
            throw new ReglaDeNegocioException(
                    "Faltan " + falta + " por pagar para cerrar la venta " + venta.getNumero());
        }
        eventos.registrar(negocioId, "Venta", ventaId, "venta_pagada",
                Map.of("negocio_id", negocioId.toString(), "venta_id", ventaId.toString(),
                        "numero", venta.getNumero(), "total", venta.getTotal()));
    }

    private String formaPagoDe(UUID ventaId) {
        List<MetodoDePago> metodos = pagos.metodosDe(ventaId);
        if (metodos.size() > 1) {
            return "MIXTO";
        }
        return metodos.contains(MetodoDePago.CREDITO) ? "CREDITO" : "CONTADO";
    }

    private static Map<String, Object> payloadCredito(UUID negocioId, Venta venta, BigDecimal monto,
            LocalDate vencimiento) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("negocio_id", negocioId.toString());
        datos.put("venta_id", venta.getId().toString());
        datos.put("cliente_id", venta.getClienteId().toString());
        datos.put("numero", venta.getNumero());
        datos.put("monto", monto);
        datos.put("fecha_vencimiento", vencimiento.toString());
        return datos;
    }

    private static BigDecimal escala(BigDecimal v) {
        return v.setScale(ESCALA, RoundingMode.HALF_UP);
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
