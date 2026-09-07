package com.regenta.inventario.aplicacion;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import com.regenta.inventario.domain.TipoProducto;

public record ProductoDelNegocio(
        UUID id,
        String sku,
        String codigoBarras,
        String nombre,
        UUID categoriaId,
        UUID unidadMedidaId,
        TipoProducto tipo,
        BigDecimal precioVenta,
        boolean manejaLotes,
        boolean perecedero,
        Map<String, Object> atributos,
        boolean activo) {
}
