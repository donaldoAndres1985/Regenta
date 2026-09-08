package com.regenta.recursos.infra;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.regenta.recursos.domain.Recurso;

public interface RecursoRepositorio extends JpaRepository<Recurso, UUID> {

    Optional<Recurso> findByIdAndNegocioId(UUID id, UUID negocioId);

    List<Recurso> findByNegocioIdAndEliminadoEnIsNullOrderByCodigoAsc(UUID negocioId);

    boolean existsByNegocioIdAndCodigo(UUID negocioId, String codigo);
}
