package com.regenta.recursos.aplicacion;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.regenta.recursos.domain.BloqueoDeRecurso;

/** Un bloqueo tal como lo ve el administrador (HU-067). */
public record BloqueoDelNegocio(
        UUID id,
        UUID recursoId,
        OffsetDateTime desde,
        OffsetDateTime hasta,
        String motivo,
        String detalle,
        UUID usuarioId,
        OffsetDateTime creadoEn) {

    static BloqueoDelNegocio de(BloqueoDeRecurso b) {
        return new BloqueoDelNegocio(b.getId(), b.getRecursoId(), b.getDesde(), b.getHasta(),
                b.getMotivo().name(), b.getDetalle(), b.getUsuarioId(), b.getCreadoEn());
    }
}
