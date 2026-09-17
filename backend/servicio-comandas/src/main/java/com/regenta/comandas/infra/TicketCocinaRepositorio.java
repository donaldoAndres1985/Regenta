package com.regenta.comandas.infra;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.regenta.comandas.domain.EstadoDeTicket;
import com.regenta.comandas.domain.TicketCocina;

public interface TicketCocinaRepositorio extends JpaRepository<TicketCocina, UUID> {

    Optional<TicketCocina> findByIdAndNegocioId(UUID id, UUID negocioId);

    List<TicketCocina> findByNegocioIdAndEstacionIdAndEstadoInOrderByPrioridadDescCreadoEnAsc(
            UUID negocioId, UUID estacionId, List<EstadoDeTicket> estados);

    int countByNegocioIdAndEstacionId(UUID negocioId, UUID estacionId);
}
