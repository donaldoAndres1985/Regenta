package com.regenta.recursos.infra;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.regenta.recursos.domain.ServicioAdicional;

public interface ServicioAdicionalRepositorio extends JpaRepository<ServicioAdicional, UUID> {

    Optional<ServicioAdicional> findByIdAndNegocioId(UUID id, UUID negocioId);

    List<ServicioAdicional> findByNegocioIdOrderByNombreAsc(UUID negocioId);

    List<ServicioAdicional> findByNegocioIdAndActivoTrueOrderByNombreAsc(UUID negocioId);

    boolean existsByNegocioIdAndCodigo(UUID negocioId, String codigo);
}
