package com.regenta.inventario.infra;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.regenta.inventario.domain.Existencia;
import com.regenta.inventario.domain.ExistenciaId;

public interface ExistenciaRepositorio extends JpaRepository<Existencia, ExistenciaId> {

    List<Existencia> findByProductoIdOrderByBodegaId(UUID productoId);

    List<Existencia> findByBodegaId(UUID bodegaId);

    Optional<Existencia> findByProductoIdAndBodegaId(UUID productoId, UUID bodegaId);

    boolean existsByBodegaId(UUID bodegaId);
}
