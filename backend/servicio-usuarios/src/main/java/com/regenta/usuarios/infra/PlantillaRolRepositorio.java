package com.regenta.usuarios.infra;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.regenta.usuarios.domain.PlantillaRol;

/** Catalogo global de plantillas de rol. */
public interface PlantillaRolRepositorio extends JpaRepository<PlantillaRol, UUID> {

    /** Las comunes a todos los patrones mas las del patron elegido. */
    List<PlantillaRol> findByPatronIsNullOrPatron(String patron);

    Optional<PlantillaRol> findByCodigo(String codigo);
}
