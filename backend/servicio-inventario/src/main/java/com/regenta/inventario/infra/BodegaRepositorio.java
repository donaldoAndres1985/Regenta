package com.regenta.inventario.infra;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.regenta.inventario.domain.Bodega;

public interface BodegaRepositorio extends JpaRepository<Bodega, UUID> {

    List<Bodega> findByNegocioIdOrderByCodigo(UUID negocioId);

    Optional<Bodega> findByNegocioIdAndCodigo(UUID negocioId, String codigo);

    Optional<Bodega> findFirstByNegocioIdAndEsDefaultTrue(UUID negocioId);

    boolean existsByNegocioId(UUID negocioId);

    long countByNegocioIdAndActivaTrue(UUID negocioId);
}
