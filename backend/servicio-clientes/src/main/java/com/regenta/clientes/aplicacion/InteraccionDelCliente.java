package com.regenta.clientes.aplicacion;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.regenta.clientes.domain.Interaccion;

/** La vista de lectura de una interaccion. */
public record InteraccionDelCliente(
        UUID id,
        String tipo,
        String asunto,
        String detalle,
        UUID autorId,
        OffsetDateTime ocurridoEn,
        LocalDate seguimientoEn) {

    static InteraccionDelCliente de(Interaccion i) {
        return new InteraccionDelCliente(i.getId(), i.getTipo().name(), i.getAsunto(),
                i.getDetalle(), i.getUsuarioId(), i.getOcurridoEn(), i.getSeguimientoEn());
    }
}
