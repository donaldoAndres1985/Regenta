package com.regenta.facturacion.aplicacion;

import java.math.BigDecimal;
import java.util.UUID;

import com.regenta.facturacion.domain.Factura;

/** Una NC vista desde la factura de origen (HU-056 criterio 4). */
public record NotaCreditoEnlazada(
        UUID id,
        String numeroCompleto,
        String codigoNota,
        BigDecimal total,
        String estado) {

    static NotaCreditoEnlazada de(Factura nc) {
        return new NotaCreditoEnlazada(nc.getId(), nc.getNumeroCompleto(), nc.getCodigoNota(),
                nc.getTotal(), nc.getEstado().name());
    }
}
