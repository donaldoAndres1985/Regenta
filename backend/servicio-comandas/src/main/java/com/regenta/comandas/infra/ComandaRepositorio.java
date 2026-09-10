package com.regenta.comandas.infra;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.regenta.comandas.domain.Comanda;
import com.regenta.comandas.domain.EstadoDeComanda;

public interface ComandaRepositorio extends JpaRepository<Comanda, UUID> {

    Optional<Comanda> findByIdAndNegocioId(UUID id, UUID negocioId);

    boolean existsBySesionMesaIdAndEstadoNotIn(UUID sesionMesaId, List<EstadoDeComanda> estados);

    Optional<Comanda> findFirstBySesionMesaIdAndEstadoNotInOrderByAbiertaEnDesc(
            UUID sesionMesaId, List<EstadoDeComanda> estados);
}
