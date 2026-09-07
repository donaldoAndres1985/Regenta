package com.regenta.inventario.infra;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import com.regenta.inventario.domain.ReservaStock;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;

public interface ReservaStockRepositorio extends JpaRepository<ReservaStock, UUID> {

    List<ReservaStock> findByNegocioIdAndOrigenTipoAndOrigenId(
            UUID negocioId, String origenTipo, UUID origenId);

    /**
     * Reservas activas que ya vencieron, bloqueadas para este barrido. El
     * {@code SKIP_LOCKED} (hint -2) deja que varias instancias barran a la vez
     * sin pelearse por las mismas filas.
     */
    @Query("select r from ReservaStock r where r.estado = 'ACTIVA' and r.expiraEn < :ahora "
            + "order by r.expiraEn")
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
    List<ReservaStock> tomarVencidas(@Param("ahora") OffsetDateTime ahora, Pageable lote);
}
