package com.regenta.reservas.aplicacion;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import com.regenta.reservas.domain.ConsumoDeEstancia;
import com.regenta.reservas.domain.Estancia;
import com.regenta.reservas.domain.Ocupante;

/**
 * Una estancia como la ve la recepción (HU-072/HU-073), con sus ocupantes y sus
 * consumos. {@code saldoConConsumos} es lo que el huésped debe al salir: el saldo
 * de la reserva más lo cargado a la habitación.
 */
public record EstanciaDelNegocio(
        UUID id,
        UUID reservaId,
        UUID recursoAsignadoId,
        String estado,
        OffsetDateTime checkInEn,
        OffsetDateTime checkOutPrevisto,
        BigDecimal consumoTotal,
        BigDecimal deposito,
        BigDecimal saldoConConsumos,
        List<OcupanteDelNegocio> ocupantes,
        List<ConsumoDelNegocio> consumos) {

    static EstanciaDelNegocio de(Estancia e, BigDecimal saldoReserva, List<Ocupante> ocupantes,
            List<ConsumoDeEstancia> consumos) {
        BigDecimal saldo = (saldoReserva == null ? BigDecimal.ZERO : saldoReserva)
                .add(e.getConsumoTotal());
        return new EstanciaDelNegocio(e.getId(), e.getReservaId(), e.getRecursoAsignadoId(),
                e.getEstado().name(), e.getCheckInEn(), e.getCheckOutPrevisto(),
                e.getConsumoTotal(), e.getDeposito(), saldo,
                ocupantes.stream().map(OcupanteDelNegocio::de).toList(),
                consumos.stream().map(ConsumoDelNegocio::de).toList());
    }
}
