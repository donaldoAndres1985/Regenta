package com.regenta.compras.aplicacion;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Las sugerencias agrupadas por proveedor preferido (HU-050 criterio 2). El
 * grupo sin proveedor lleva {@code sinProveedor = true} y hay que asignarle uno
 * antes de poder crear la orden (criterio 4).
 */
public record GrupoDeSugerencias(
        UUID proveedorId,
        String proveedorRazonSocial,
        boolean sinProveedor,
        BigDecimal costoTotalEstimado,
        List<SugerenciaDelNegocio> sugerencias) {
}
