package com.regenta.usuarios.infra;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.regenta.usuarios.domain.Rol;

public interface RolRepositorio extends JpaRepository<Rol, UUID> {

    List<Rol> findByNegocioIdOrderByNombre(UUID negocioId);

    Optional<Rol> findByNegocioIdAndNombre(UUID negocioId, String nombre);

    Optional<Rol> findByNegocioIdAndPlantillaId(UUID negocioId, UUID plantillaId);
}
