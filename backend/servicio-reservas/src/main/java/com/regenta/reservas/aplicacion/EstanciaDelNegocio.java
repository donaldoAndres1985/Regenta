package com.regenta.reservas.aplicacion;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import com.regenta.reservas.domain.Estancia;
import com.regenta.reservas.domain.Ocupante;

/** Una estancia como la ve la recepción (HU-072), con sus ocupantes. */
public record EstanciaDelNegocio(
        UUID id,
        UUID reservaId,
        UUID recursoAsignadoId,
        String estado,
        OffsetDateTime checkInEn,
        OffsetDateTime checkOutPrevisto,
        BigDecimal consumoTotal,
        BigDecimal deposito,
        List<OcupanteDelNegocio> ocupantes) {

    static EstanciaDelNegocio de(Estancia e, List<Ocupante> ocupantes) {
        return new EstanciaDelNegocio(e.getId(), e.getReservaId(), e.getRecursoAsignadoId(),
                e.getEstado().name(), e.getCheckInEn(), e.getCheckOutPrevisto(),
                e.getConsumoTotal(), e.getDeposito(),
                ocupantes.stream().map(OcupanteDelNegocio::de).toList());
    }
}
