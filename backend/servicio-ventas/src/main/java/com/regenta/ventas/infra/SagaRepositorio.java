package com.regenta.ventas.infra;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import com.regenta.ventas.domain.Saga;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;

public interface SagaRepositorio extends JpaRepository<Saga, UUID> {

    Optional<Saga> findByCorrelacionId(UUID correlacionId);

    Optional<Saga> findByAgregadoId(UUID agregadoId);

    /**
     * Sagas que siguen esperando stock y ya vencieron, bloqueadas para este
     * barrido. El {@code SKIP_LOCKED} (hint -2) deja que varias instancias
     * barran a la vez sin pelearse por las mismas filas.
     */
    @Query("select s from Saga s where s.estado = com.regenta.ventas.domain.EstadoSaga.ESPERANDO_STOCK "
            + "and s.timeoutEn < :ahora order by s.timeoutEn")
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
    List<Saga> tomarVencidas(@Param("ahora") OffsetDateTime ahora);
}
