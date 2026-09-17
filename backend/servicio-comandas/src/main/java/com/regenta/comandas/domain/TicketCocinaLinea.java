package com.regenta.comandas.domain;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

/**
 * Qué línea de la comanda va en qué ticket de cocina (HU-088 criterio 1): un
 * ticket lleva solo las líneas de su propia estación.
 */
@Entity
@Table(name = "ticket_cocina_lineas")
@IdClass(TicketCocinaLineaId.class)
public class TicketCocinaLinea {

    @Id
    @Column(name = "ticket_id", nullable = false, updatable = false)
    private UUID ticketId;

    @Id
    @Column(name = "linea_id", nullable = false, updatable = false)
    private UUID lineaId;

    @Column(name = "negocio_id", nullable = false, updatable = false)
    private UUID negocioId;

    protected TicketCocinaLinea() {
    }

    public static TicketCocinaLinea de(UUID negocioId, UUID ticketId, UUID lineaId) {
        TicketCocinaLinea t = new TicketCocinaLinea();
        t.negocioId = negocioId;
        t.ticketId = ticketId;
        t.lineaId = lineaId;
        return t;
    }

    public UUID getTicketId() {
        return ticketId;
    }

    public UUID getLineaId() {
        return lineaId;
    }

    public UUID getNegocioId() {
        return negocioId;
    }
}
