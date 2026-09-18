package com.regenta.auditoria.infra;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.regenta.auditoria.domain.ConflictoSync;

public interface ConflictoSyncRepositorio extends JpaRepository<ConflictoSync, UUID> {

    List<ConflictoSync> findByNegocioIdAndResolucionIsNullOrderByDetectadoEnDesc(UUID negocioId);
}
