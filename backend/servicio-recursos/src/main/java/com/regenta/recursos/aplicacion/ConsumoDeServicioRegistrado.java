package com.regenta.recursos.aplicacion;

import java.util.UUID;

/**
 * El resultado de consumir un servicio adicional en una reserva (HU-068 criterio
 * 2). Si el servicio está enlazado a un producto, se publica
 * {@code servicio_adicional_consumido} para que servicio-inventario descuente
 * stock; {@code descuentaInventario} dice si eso ocurrió.
 */
public record ConsumoDeServicioRegistrado(
        CotizacionDeServicio cotizacion,
        boolean descuentaInventario,
        UUID productoId) {
}
