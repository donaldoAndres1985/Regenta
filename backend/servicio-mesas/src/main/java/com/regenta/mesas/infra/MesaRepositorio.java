package com.regenta.mesas.infra;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.regenta.mesas.domain.Mesa;

public interface MesaRepositorio extends JpaRepository<Mesa, UUID> {

    Optional<Mesa> findByIdAndNegocioId(UUID id, UUID negocioId);

    List<Mesa> findByNegocioIdAndActivaTrueOrderByCodigoAsc(UUID negocioId);

    List<Mesa> findByNegocioIdAndZonaIdAndActivaTrueOrderByCodigoAsc(UUID negocioId, UUID zonaId);

    long countByNegocioIdAndZonaIdAndActivaTrue(UUID negocioId, UUID zonaId);

    boolean existsByNegocioIdAndCodigo(UUID negocioId, String codigo);
}
