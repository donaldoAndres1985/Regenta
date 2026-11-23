package com.regenta.alertas.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.regenta.alertas.domain.DispositivoPush;

public interface DispositivoPushRepositorio extends JpaRepository<DispositivoPush, UUID> {

    /** HU-123: el token es único por negocio (alertas V6). */
    Optional<DispositivoPush> findByNegocioIdAndTokenFcm(UUID negocioId, String tokenFcm);

    Optional<DispositivoPush> findByIdAndNegocioId(UUID id, UUID negocioId);

    List<DispositivoPush> findByNegocioIdAndUsuarioIdAndActivoTrue(UUID negocioId, UUID usuarioId);
}
