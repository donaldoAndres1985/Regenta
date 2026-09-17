package com.regenta.comandas.aplicacion;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/** Un ticket de cocina con sus líneas, para la pantalla KDS (HU-088). */
public record TicketDetallado(
        UUID id,
        UUID comandaId,
        String comandaNumero,
        UUID estacionId,
        int secuencia,
        String estado,
        OffsetDateTime creadoEn,
        int minutosTranscurridos,
        boolean demorado,
        List<LineaDeTicket> lineas) {

    public record LineaDeTicket(UUID id, String nombre, BigDecimal cantidad, String notas) {
    }
}
