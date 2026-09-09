package com.regenta.menu.infra;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.regenta.menu.domain.ItemGrupoModificador;
import com.regenta.menu.domain.ItemGrupoModificadorId;

public interface ItemGrupoModificadorRepositorio
        extends JpaRepository<ItemGrupoModificador, ItemGrupoModificadorId> {

    List<ItemGrupoModificador> findByItemIdOrderByOrdenAsc(UUID itemId);

    boolean existsByItemIdAndGrupoId(UUID itemId, UUID grupoId);

    void deleteByItemIdAndGrupoId(UUID itemId, UUID grupoId);
}
