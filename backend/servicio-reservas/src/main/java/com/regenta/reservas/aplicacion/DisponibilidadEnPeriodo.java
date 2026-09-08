package com.regenta.reservas.aplicacion;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * La respuesta de "qué hay libre entre estas dos fechas" (HU-069). {@code libres}
 * son los recursos reservables sin reservas ni bloqueos que pisen el periodo (ya
 * con el buffer del tipo aplicado); {@code ocupados} dice por qué queda fuera
 * cada uno.
 */
public record DisponibilidadEnPeriodo(
        OffsetDateTime desde,
        OffsetDateTime hasta,
        int totalRecursos,
        List<RecursoLibre> libres,
        List<RecursoOcupado> ocupados) {

    public record RecursoLibre(UUID recursoId, String codigo, String nombre, int capacidad) {
    }

    /** {@code motivo} es {@code RESERVA} o {@code BLOQUEO}. */
    public record RecursoOcupado(UUID recursoId, String codigo, String motivo) {
    }
}
