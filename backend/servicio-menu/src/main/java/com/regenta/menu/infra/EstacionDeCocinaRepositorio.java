package com.regenta.menu.infra;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.regenta.menu.domain.EstacionDeCocina;

public interface EstacionDeCocinaRepositorio extends JpaRepository<EstacionDeCocina, UUID> {

    Optional<EstacionDeCocina> findByIdAndNegocioId(UUID id, UUID negocioId);

    List<EstacionDeCocina> findByNegocioIdOrderByOrdenAscNombreAsc(UUID negocioId);

    boolean existsByNegocioIdAndCodigo(UUID negocioId, String codigo);
}
