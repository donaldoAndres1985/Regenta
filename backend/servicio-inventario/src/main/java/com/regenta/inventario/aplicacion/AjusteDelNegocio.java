package com.regenta.inventario.aplicacion;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import com.regenta.inventario.domain.EstadoAjuste;
import com.regenta.inventario.domain.TipoAjuste;

public record AjusteDelNegocio(
        UUID id,
        String numero,
        UUID bodegaId,
        TipoAjuste tipo,
        EstadoAjuste estado,
        String motivo,
        UUID usuarioId,
        UUID aprobadoPor,
        OffsetDateTime fecha,
        List<LineaDelAjuste> lineas) {
}
