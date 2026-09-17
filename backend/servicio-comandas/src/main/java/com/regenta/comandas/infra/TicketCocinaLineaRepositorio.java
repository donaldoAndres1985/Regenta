package com.regenta.comandas.infra;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.regenta.comandas.domain.TicketCocinaLinea;
import com.regenta.comandas.domain.TicketCocinaLineaId;

public interface TicketCocinaLineaRepositorio
        extends JpaRepository<TicketCocinaLinea, TicketCocinaLineaId> {

    List<TicketCocinaLinea> findByTicketIdIn(List<UUID> ticketIds);
}
