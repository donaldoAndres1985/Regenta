package com.regenta.facturacion.aplicacion;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.regenta.facturacion.domain.Contingencia;

/** Una contingencia y cuántas facturas quedaron afectadas (HU-057 criterio 4). */
public record ContingenciaDelNegocio(
        UUID id,
        OffsetDateTime inicioEn,
        OffsetDateTime finEn,
        String motivo,
        int facturasAfectadas,
        boolean abierta,
        boolean regularizada) {

    static ContingenciaDelNegocio de(Contingencia c) {
        return new ContingenciaDelNegocio(c.getId(), c.getInicioEn(), c.getFinEn(), c.getMotivo(),
                c.getFacturasAfectadas(), c.estaAbierta(), c.isRegularizada());
    }
}
