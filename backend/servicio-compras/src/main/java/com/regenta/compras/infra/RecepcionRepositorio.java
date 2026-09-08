package com.regenta.compras.infra;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.regenta.compras.domain.Recepcion;

public interface RecepcionRepositorio extends JpaRepository<Recepcion, UUID> {

    Optional<Recepcion> findByIdAndNegocioId(UUID id, UUID negocioId);

    List<Recepcion> findByNegocioIdOrderByFechaDesc(UUID negocioId);

    List<Recepcion> findByNegocioIdAndOrdenIdOrderByFechaDesc(UUID negocioId, UUID ordenId);
}
