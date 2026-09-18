package com.regenta.auditoria.infra;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.regenta.auditoria.domain.ConflictoSync;

public interface ConflictoSyncRepositorio extends JpaRepository<ConflictoSync, UUID> {

    List<ConflictoSync> findByNegocioIdAndResolucionIsNullOrderByDetectadoEnDesc(UUID negocioId);

    /** HU-103 criterio 5: los que llevan sin resolver desde antes de {@code limite}. */
    List<ConflictoSync> findByNegocioIdAndResolucionIsNullAndDetectadoEnBefore(UUID negocioId,
            OffsetDateTime limite);
}
