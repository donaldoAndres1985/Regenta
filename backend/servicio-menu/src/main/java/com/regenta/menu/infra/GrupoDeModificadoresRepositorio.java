package com.regenta.menu.infra;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.regenta.menu.domain.GrupoDeModificadores;

public interface GrupoDeModificadoresRepositorio extends JpaRepository<GrupoDeModificadores, UUID> {

    Optional<GrupoDeModificadores> findByIdAndNegocioId(UUID id, UUID negocioId);

    List<GrupoDeModificadores> findByNegocioIdOrderByNombreAsc(UUID negocioId);

    List<GrupoDeModificadores> findByIdIn(Collection<UUID> ids);
}
