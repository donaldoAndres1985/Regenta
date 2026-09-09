package com.regenta.menu.infra;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.regenta.menu.domain.Modificador;

public interface ModificadorRepositorio extends JpaRepository<Modificador, UUID> {

    Optional<Modificador> findByIdAndNegocioId(UUID id, UUID negocioId);

    List<Modificador> findByGrupoIdOrderByOrdenAscNombreAsc(UUID grupoId);

    List<Modificador> findByGrupoIdInOrderByOrdenAscNombreAsc(Collection<UUID> grupoIds);

    List<Modificador> findByIdInAndNegocioId(Collection<UUID> ids, UUID negocioId);
}
