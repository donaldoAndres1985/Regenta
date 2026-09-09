package com.regenta.menu.infra;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.regenta.menu.domain.CategoriaMenu;

public interface CategoriaMenuRepositorio extends JpaRepository<CategoriaMenu, UUID> {

    Optional<CategoriaMenu> findByIdAndNegocioId(UUID id, UUID negocioId);

    List<CategoriaMenu> findByCartaIdOrderByOrdenAscNombreAsc(UUID cartaId);
}
