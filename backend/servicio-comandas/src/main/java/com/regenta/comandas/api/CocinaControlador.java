package com.regenta.comandas.api;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.regenta.comandas.aplicacion.GestionDeCocina;
import com.regenta.comandas.aplicacion.TicketDetallado;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/** El KDS: tickets de cocina agrupados por estación. HU-088. */
@RestController
@RequestMapping("/api/cocina")
@Tag(name = "Cocina", description = "Tickets de cocina por estación, agrupados por columna")
public class CocinaControlador {

    private final GestionDeCocina cocina;

    public CocinaControlador(GestionDeCocina cocina) {
        this.cocina = cocina;
    }

    @GetMapping("/estaciones/{estacionId}/tickets")
    @Operation(summary = "Los tickets vivos de una estación, para sus columnas Nuevos/En preparación/Listos")
    public List<TicketDetallado> ticketsDeEstacion(@PathVariable UUID estacionId) {
        return cocina.ticketsDeEstacion(estacionId);
    }

    @PostMapping("/tickets/{ticketId}/avance")
    @Operation(summary = "Avanza el ticket (NUEVO → EN_PREPARACION → LISTO → ENTREGADO)")
    public TicketDetallado avanzarTicket(@PathVariable UUID ticketId) {
        return cocina.avanzarTicket(ticketId);
    }
}
