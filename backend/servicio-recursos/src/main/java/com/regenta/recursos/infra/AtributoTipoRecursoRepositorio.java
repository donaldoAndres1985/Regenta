package com.regenta.recursos.infra;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.regenta.recursos.domain.AtributoTipoRecurso;

public interface AtributoTipoRecursoRepositorio extends JpaRepository<AtributoTipoRecurso, UUID> {

    List<AtributoTipoRecurso> findByTipoRecursoIdOrderByOrdenAscNombreCampoAsc(UUID tipoRecursoId);

    Optional<AtributoTipoRecurso> findByIdAndNegocioId(UUID id, UUID negocioId);

    boolean existsByTipoRecursoIdAndNombreCampo(UUID tipoRecursoId, String nombreCampo);
}
