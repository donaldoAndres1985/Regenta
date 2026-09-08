package com.regenta.recursos.infra;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.regenta.recursos.domain.TipoDeRecurso;

public interface TipoDeRecursoRepositorio extends JpaRepository<TipoDeRecurso, UUID> {

    Optional<TipoDeRecurso> findByIdAndNegocioId(UUID id, UUID negocioId);

    List<TipoDeRecurso> findByNegocioIdAndActivoTrueOrderByNombreAsc(UUID negocioId);

    boolean existsByNegocioIdAndNombre(UUID negocioId, String nombre);
}
