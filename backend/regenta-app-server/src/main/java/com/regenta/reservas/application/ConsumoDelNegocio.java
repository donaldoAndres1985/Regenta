package com.regenta.reservas.application;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.regenta.reservas.domain.ConsumoDeEstancia;

/** Un cargo a la habitaciÃ³n como lo ve la recepciÃ³n (HU-073). */
public record ConsumoDelNegocio(
        UUID id,
        String origen,
        UUID productoId,
        UUID comandaId,
        String descripcion,
        BigDecimal cantidad,
        BigDecimal precioUnitario,
        BigDecimal impuestoPct,
        BigDecimal total,
        OffsetDateTime cargadoEn) {

    static ConsumoDelNegocio de(ConsumoDeEstancia c) {
        return new ConsumoDelNegocio(c.getId(), c.getOrigen().name(), c.getProductoId(),
                c.getComandaId(), c.getDescripcion(), c.getCantidad(), c.getPrecioUnitario(),
                c.getImpuestoPct(), c.getTotal(), c.getCargadoEn());
    }
}

