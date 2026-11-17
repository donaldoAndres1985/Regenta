package com.regenta.inventario.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.regenta.inventario.domain.EquivalenciaDeDetallado;

/** HU-161 · equivalencias de detallado del negocio. */
public interface EquivalenciaDeDetalladoRepositorio extends JpaRepository<EquivalenciaDeDetallado, UUID> {

    boolean existsByOrigenIdAndDestinoId(UUID origenId, UUID destinoId);

    List<EquivalenciaDeDetallado> findByOrigenId(UUID origenId);

    List<EquivalenciaDeDetallado> findByDestinoId(UUID destinoId);

    Optional<EquivalenciaDeDetallado> findFirstByOrigenId(UUID origenId);

    List<EquivalenciaDeDetallado> findByNegocioId(UUID negocioId);
}
