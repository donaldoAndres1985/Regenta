package com.regenta.menu.infra;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.regenta.menu.domain.ItemDeMenu;

public interface ItemDeMenuRepositorio extends JpaRepository<ItemDeMenu, UUID> {

    Optional<ItemDeMenu> findByIdAndNegocioId(UUID id, UUID negocioId);

    List<ItemDeMenu> findByCategoriaMenuIdAndEliminadoEnIsNullOrderByOrdenAscNombreAsc(
            UUID categoriaMenuId);

    List<ItemDeMenu> findByCategoriaMenuIdInAndEliminadoEnIsNullOrderByOrdenAscNombreAsc(
            Collection<UUID> categoriaMenuIds);

    boolean existsByNegocioIdAndCodigo(UUID negocioId, String codigo);
}
