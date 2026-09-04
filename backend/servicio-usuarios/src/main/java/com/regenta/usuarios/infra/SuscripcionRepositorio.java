package com.regenta.usuarios.infra;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.regenta.usuarios.domain.Suscripcion;

public interface SuscripcionRepositorio extends JpaRepository<Suscripcion, UUID> {

    Optional<Suscripcion> findByNegocioIdAndFechaFinIsNull(UUID negocioId);
}
