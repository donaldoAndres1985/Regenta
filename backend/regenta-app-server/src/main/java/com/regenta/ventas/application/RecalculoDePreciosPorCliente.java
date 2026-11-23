package com.regenta.ventas.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.regenta.inventario.api.ListaDePreciosInfo;
import com.regenta.shared.web.NoEncontradoException;
import com.regenta.ventas.domain.Venta;
import com.regenta.ventas.domain.VentaLinea;
import com.regenta.ventas.infrastructure.VentaLineaRepositorio;

/**
 * HU-132 · R7 de {@code design/comportamiento/ClienteVenta.md}: cambiar de cliente recalcula los
 * precios que dependen de él. Primero se prevé (C2: se avisa antes) y solo después, si se acepta, se
 * aplica (C1). Sirve igual para una venta en borrador del servidor y para el carrito del POS, que
 * vive en el equipo hasta cobrar.
 *
 * <p>La lista del negocio son los precios del catálogo: lo que cobra toda venta sin lista (y lo que
 * muestra el POS al buscar). Solo un cliente con lista propia, existente y vigente, la cambia (C4).
 *
 * <p>Se recalcula solo lo que salió de la lista de la venta: una línea cuyo precio no es el que hoy
 * da esa lista (una fracción, una cotización convertida) se deja como está, y también la que tiene
 * un descuento escrito a mano (C3): ni su precio ni su descuento se tocan.
 */
@Component
public class RecalculoDePreciosPorCliente {

    static final String DESCUENTO_A_MANO = "DESCUENTO_A_MANO";
    static final String PRECIO_ESPECIAL = "PRECIO_ESPECIAL";
    private static final BigDecimal CIEN = new BigDecimal("100");

    private final VentaLineaRepositorio lineas;
    private final PrecioDeLinea precios;

    RecalculoDePreciosPorCliente(VentaLineaRepositorio lineas, PrecioDeLinea precios) {
        this.lineas = lineas;
        this.precios = precios;
    }

    /** Una lista de precios por su nombre; {@code id} nulo: los precios del negocio (catálogo). */
    public record ListaDeLaVenta(UUID id, String nombre) {
    }

    /** Una línea que cambiaría de precio. */
    public record CambioDePrecio(int linea, UUID productoId, String producto, BigDecimal cantidad,
            BigDecimal precioActual, BigDecimal precioNuevo) {
    }

    /** Una línea que se queda como está y por qué: {@code DESCUENTO_A_MANO} o {@code PRECIO_ESPECIAL}. */
    public record LineaQueNoCambia(int linea, String producto, String motivo) {
    }

    /** C2 · Lo que pasaría con los precios si se asigna ese cliente. */
    public record PrevisionDePrecios(ListaDeLaVenta listaActual, ListaDeLaVenta listaNueva,
            List<CambioDePrecio> cambios, List<LineaQueNoCambia> sinCambio, BigDecimal totalActual,
            BigDecimal totalNuevo, boolean hayCambios) {
    }

    /** Una línea a repreciar, venga de una venta guardada o del carrito del POS. */
    public record LineaAPreciar(int linea, UUID productoId, String producto, BigDecimal cantidad,
            BigDecimal precioUnitario, BigDecimal descuentoPct) {

        BigDecimal total() {
            BigDecimal bruto = precioUnitario.multiply(cantidad);
            BigDecimal pct = descuentoPct == null ? BigDecimal.ZERO : descuentoPct;
            return bruto.subtract(bruto.multiply(pct).divide(CIEN, 4, RoundingMode.HALF_UP));
        }
    }

    /** La lista con que se le vende a un cliente: la suya si existe y está vigente; si no, ninguna. */
    UUID listaPara(UUID negocioId, UUID listaDelCliente) {
        return precios.lista(negocioId, listaDelCliente).map(ListaDePreciosInfo::id).orElse(null);
    }

    /** Lo que quedaría de una venta guardada con la lista del cliente, sin guardar nada. */
    PrevisionDePrecios prever(Venta venta, UUID listaDelCliente) {
        List<LineaAPreciar> deLaVenta = lineas.findByVentaIdOrderByLinea(venta.getId()).stream()
                .map(l -> new LineaAPreciar(l.getLinea(), l.getProductoId(), l.getNombreSnapshot(), l.getCantidad(),
                        l.getPrecioUnitario(), l.getDescuentoPct()))
                .toList();
        return prever(venta.getNegocioId(), venta.getListaPreciosId(), listaDelCliente, deLaVenta);
    }

    /** Lo que quedaría de unas líneas (las del carrito del POS) con la lista del cliente. */
    PrevisionDePrecios prever(UUID negocio, UUID listaActual, UUID listaDelCliente, List<LineaAPreciar> aPreciar) {
        UUID idNueva = listaPara(negocio, listaDelCliente);
        ListaDeLaVenta actual = new ListaDeLaVenta(listaActual, nombre(negocio, listaActual));
        ListaDeLaVenta destino = new ListaDeLaVenta(idNueva, nombre(negocio, idNueva));

        List<CambioDePrecio> cambios = new ArrayList<>();
        List<LineaQueNoCambia> sinCambio = new ArrayList<>();
        BigDecimal totalActual = BigDecimal.ZERO;
        BigDecimal diferencia = BigDecimal.ZERO;
        for (LineaAPreciar l : aPreciar) {
            totalActual = totalActual.add(l.total());
            if (Objects.equals(idNueva, listaActual)) {
                continue; // C4: misma lista, mismos precios.
            }
            if (l.descuentoPct() != null && l.descuentoPct().signum() > 0) {
                sinCambio.add(new LineaQueNoCambia(l.linea(), l.producto(), DESCUENTO_A_MANO));
                continue;
            }
            BigDecimal segunLaVenta = precio(negocio, l, listaActual);
            if (segunLaVenta == null || segunLaVenta.compareTo(l.precioUnitario()) != 0) {
                sinCambio.add(new LineaQueNoCambia(l.linea(), l.producto(), PRECIO_ESPECIAL));
                continue;
            }
            BigDecimal nuevo = precio(negocio, l, idNueva);
            if (nuevo != null && nuevo.compareTo(l.precioUnitario()) != 0) {
                cambios.add(new CambioDePrecio(l.linea(), l.productoId(), l.producto(), l.cantidad(),
                        l.precioUnitario(), nuevo));
                // Sin descuento, el total de la línea es cantidad × precio con IVA incluido (HU-148).
                diferencia = diferencia.add(nuevo.subtract(l.precioUnitario()).multiply(l.cantidad()));
            }
        }
        return new PrevisionDePrecios(actual, destino, cambios, sinCambio, totalActual,
                totalActual.add(diferencia), !cambios.isEmpty());
    }

    /** C1 · Aplica la previsión a una venta guardada: pasa a la lista nueva y sus líneas, a su precio. */
    void aplicar(Venta venta, PrevisionDePrecios prevision) {
        venta.usarListaDePrecios(prevision.listaNueva().id());
        for (CambioDePrecio c : prevision.cambios()) {
            lineas.findByVentaIdAndLinea(venta.getId(), (short) c.linea()).ifPresent(l -> {
                l.cambiarPrecio(c.precioNuevo());
                lineas.save(l);
            });
        }
    }

    private String nombre(UUID negocio, UUID lista) {
        return lista == null ? null : precios.lista(negocio, lista).map(ListaDePreciosInfo::nombre).orElse(null);
    }

    private BigDecimal precio(UUID negocio, LineaAPreciar l, UUID lista) {
        try {
            return precios.de(negocio, l.productoId(), lista, l.cantidad()).precio();
        } catch (NoEncontradoException fueraDelCatalogo) {
            return null;
        }
    }
}
