package com.regenta.mesas.infra;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.regenta.mesas.domain.Zona;

public interface ZonaRepositorio extends JpaRepository<Zona, UUID> {

    Optional<Zona> findByIdAndNegocioId(UUID id, UUID negocioId);

    List<Zona> findByNegocioIdOrderByOrdenAscNombreAsc(UUID negocioId);

    boolean existsByNegocioIdAndNombreIgnoreCase(UUID negocioId, String nombre);
}
