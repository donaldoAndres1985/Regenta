package com.regenta.ventas.application;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.shared.security.RequierePermiso;
import com.regenta.shared.tenancy.ContextoDeNegocio;
import com.regenta.ventas.application.RecalculoDePreciosPorCliente.LineaAPreciar;
import com.regenta.ventas.application.RecalculoDePreciosPorCliente.PrevisionDePrecios;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * HU-132 · R7 para el carrito del POS, que vive en el equipo hasta cobrar. Antes de asignar un cliente
 * se prevé qué precios cambian con su lista (C2); si se acepta, el carrito toma esa lista y la manda al
 * cobrar, y el backend cobra con ella (HU-148 I4: el precio lo sigue decidiendo el backend). Los
 * productos que se agreguen después se precian con la misma lista.
 */
@Service
public class PreciosDelCarrito {

    private final ConsultaDeClientes clientes;
    private final RecalculoDePreciosPorCliente recalculo;
    private final PrecioDeLinea precios;

    public PreciosDelCarrito(ConsultaDeClientes clientes, RecalculoDePreciosPorCliente recalculo,
            PrecioDeLinea precios) {
        this.clientes = clientes;
        this.recalculo = recalculo;
        this.precios = precios;
    }

    /** Una línea del carrito: el precio que muestra hoy (IVA incluido) y su descuento a mano. */
    public record LineaDelCarrito(int linea, @NotNull UUID productoId, String nombre,
            @NotNull @Positive BigDecimal cantidad, @NotNull @PositiveOrZero BigDecimal precioUnitario,
            @PositiveOrZero BigDecimal descuentoPct) {
    }

    /** {@code clienteId} nulo: volver a consumidor final; {@code listaPreciosId}: con la que está el carrito. */
    public record SolicitudDePrevision(UUID clienteId, UUID listaPreciosId,
            @NotNull List<@Valid LineaDelCarrito> lineas) {
    }

    public record ProductoAPreciar(@NotNull UUID productoId, @NotNull @Positive BigDecimal cantidad) {
    }

    public record SolicitudDePrecios(UUID listaPreciosId, @NotNull List<@Valid ProductoAPreciar> productos) {
    }

    public record PrecioDelProducto(UUID productoId, BigDecimal cantidad, BigDecimal precio) {
    }

    @Transactional(readOnly = true)
    @RequierePermiso("VENTAS_VENTA_CREAR")
    public PrevisionDePrecios prever(SolicitudDePrevision s) {
        UUID negocio = ContextoDeNegocio.negocioActual();
        UUID listaDelCliente = s.clienteId() == null ? null : clientes.consultar(s.clienteId()).listaPreciosId();
        List<LineaAPreciar> lineas = s.lineas().stream()
                .map(l -> new LineaAPreciar(l.linea(), l.productoId(), l.nombre(), l.cantidad(), l.precioUnitario(),
                        l.descuentoPct()))
                .toList();
        return recalculo.prever(negocio, s.listaPreciosId(), listaDelCliente, lineas);
    }

    /** El precio de cada producto con la lista del carrito (con su tramo por volumen) o el del catálogo. */
    @Transactional(readOnly = true)
    @RequierePermiso("VENTAS_VENTA_CREAR")
    public List<PrecioDelProducto> precios(SolicitudDePrecios s) {
        UUID negocio = ContextoDeNegocio.negocioActual();
        return s.productos().stream()
                .map(p -> new PrecioDelProducto(p.productoId(), p.cantidad(),
                        precios.de(negocio, p.productoId(), s.listaPreciosId(), p.cantidad()).precio()))
                .toList();
    }
}
