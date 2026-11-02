package com.regenta.comandas.domain;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/** Clave compuesta de {@link TicketCocinaLinea}: {@code (ticket_id, linea_id)}. */
public class TicketCocinaLineaId implements Serializable {

    private static final long serialVersionUID = 1L;

    private UUID ticketId;
    private UUID lineaId;

    public TicketCocinaLineaId() {
    }

    public TicketCocinaLineaId(UUID ticketId, UUID lineaId) {
        this.ticketId = ticketId;
        this.lineaId = lineaId;
    }

    public UUID getTicketId() {
        return ticketId;
    }

    public UUID getLineaId() {
        return lineaId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TicketCocinaLineaId otra)) {
            return false;
        }
        return Objects.equals(ticketId, otra.ticketId) && Objects.equals(lineaId, otra.lineaId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ticketId, lineaId);
    }
}
