package com.regenta.auditoria.infra;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.regenta.auditoria.domain.OperacionSync;

public interface OperacionSyncRepositorio extends JpaRepository<OperacionSync, UUID> {

    Optional<OperacionSync> findByNegocioIdAndIdempotencyKey(UUID negocioId, String idempotencyKey);
}
