package com.regenta.alertas.infra;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.regenta.alertas.domain.DispositivoPush;

public interface DispositivoPushRepositorio extends JpaRepository<DispositivoPush, UUID> {

    Optional<DispositivoPush> findByTokenFcm(String tokenFcm);

    Optional<DispositivoPush> findByIdAndNegocioId(UUID id, UUID negocioId);

    List<DispositivoPush> findByNegocioIdAndUsuarioIdAndActivoTrue(UUID negocioId, UUID usuarioId);
}
