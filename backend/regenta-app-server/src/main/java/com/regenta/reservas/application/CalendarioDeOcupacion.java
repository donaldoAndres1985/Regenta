package com.regenta.reservas.application;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * La ocupaciÃ³n de todos los recursos en una franja de dÃ­as (HU-075): una fila
 * por recurso, una columna por dÃ­a, las reservas como barras y los bloqueos
 * aparte.
 */
public record CalendarioDeOcupacion(
        LocalDate desde,
        LocalDate hasta,
        List<LocalDate> dias,
        List<RecursoDeCalendario> recursos,
        List<BarraDeReserva> reservas,
        List<BarraDeBloqueo> bloqueos) {

    public record RecursoDeCalendario(UUID id, String codigo, String nombre, int capacidad) {
    }

    public record BarraDeReserva(UUID id, UUID recursoId, String numero, String estado,
            OffsetDateTime desde, OffsetDateTime hasta) {
    }

    public record BarraDeBloqueo(UUID recursoId, OffsetDateTime desde, OffsetDateTime hasta,
            String motivo) {
    }
}

