package com.regenta.compras.application;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Las sugerencias agrupadas por proveedor preferido (HU-050 criterio 2). El
 * grupo sin proveedor lleva {@code sinProveedor = true} y hay que asignarle uno
 * antes de poder crear la orden (criterio 4).
 *
 * <p>HU-181 · I31: un grupo es de un proveedor <b>y</b> una bodega, porque aceptarlo crea una
 * sola orden y la orden llega a una bodega. El de «Sin proveedor» junta todas las bodegas.
 */
public record GrupoDeSugerencias(
        UUID proveedorId,
        String proveedorRazonSocial,
        UUID bodegaId,
        boolean sinProveedor,
        BigDecimal costoTotalEstimado,
        List<SugerenciaDelNegocio> sugerencias) {
}
