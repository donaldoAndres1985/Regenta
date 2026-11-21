package com.regenta.compras.application;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * HU-180 · Un producto que llega por primera vez, creado sin salir de la recepción (I21). Los campos
 * del producto llevan las mismas reglas que la ficha de Inventario (HU-147), así que se rechaza con
 * los mismos mensajes (C2). Lo del proveedor (su código y el costo) va a su catálogo (C3).
 */
public record SolicitudDeProductoNuevo(
        @NotBlank @Size(max = 60) String sku,
        @Size(max = 60) String codigoBarras,
        @NotBlank @Size(max = 180) String nombre,
        @NotNull UUID categoriaId,
        @NotNull UUID unidadMedidaId,
        /** Obligatoria; sin ella Inventario responde «Elige la tarifa de IVA del producto.». */
        String tarifaIva,
        /** Con IVA incluido (Inventario I2). */
        @PositiveOrZero BigDecimal precioVenta,
        boolean manejaLotes,
        boolean perecedero,
        @Size(max = 60) String codigoProveedor,
        @PositiveOrZero BigDecimal costo) {
}
