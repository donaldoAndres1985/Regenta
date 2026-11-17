package com.regenta.inventario.application;

import com.regenta.inventario.api.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import com.regenta.inventario.domain.TarifaIva;
import com.regenta.inventario.domain.TipoProducto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** Alta o edicion de un producto. Los campos variables van en {@code atributos}. */
public record SolicitudDeProducto(
        @NotBlank @Size(max = 60) String sku,
        @Size(max = 60) String codigoBarras,
        @NotBlank @Size(max = 180) String nombre,
        @Size(max = 4000) String descripcion,
        @NotNull UUID categoriaId,
        @NotNull UUID unidadMedidaId,
        UUID marcaId,
        TipoProducto tipo,
        /** HU-147: el precio de la etiqueta, con IVA incluido (I2). */
        @PositiveOrZero BigDecimal precioVenta,
        /** HU-147 C1: obligatoria; sin ella se rechaza con la regla de negocio. */
        TarifaIva tarifaIva,
        Boolean controlaStock,
        BigDecimal stockMinimo,
        BigDecimal stockMaximo,
        boolean manejaLotes,
        boolean manejaSeries,
        boolean perecedero,
        boolean permiteVentaSinStock,
        Map<String, Object> atributos) {
}
