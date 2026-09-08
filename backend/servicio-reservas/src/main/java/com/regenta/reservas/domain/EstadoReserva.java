package com.regenta.reservas.domain;

/**
 * El ciclo de vida de una reserva. Solo tres estados ocupan físicamente el
 * recurso; son los mismos que filtra el {@code EXCLUDE USING gist} de la tabla y
 * los únicos que una consulta de disponibilidad tiene que descontar (HU-069
 * criterio 2: una {@code CANCELADA} o {@code NO_SHOW} deja el recurso libre).
 */
public enum EstadoReserva {

    PENDIENTE(true),
    CONFIRMADA(true),
    CHECK_IN(true),
    CHECK_OUT(false),
    CANCELADA(false),
    NO_SHOW(false),
    EXPIRADA(false);

    private final boolean ocupa;

    EstadoReserva(boolean ocupa) {
        this.ocupa = ocupa;
    }

    public boolean ocupaRecurso() {
        return ocupa;
    }
}
