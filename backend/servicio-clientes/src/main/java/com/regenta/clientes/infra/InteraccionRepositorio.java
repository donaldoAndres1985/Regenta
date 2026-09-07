package com.regenta.clientes.infra;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import com.regenta.clientes.domain.Interaccion;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;

public interface InteraccionRepositorio extends JpaRepository<Interaccion, UUID> {

    /** Criterio 2: la ficha las muestra de la mas reciente a la mas antigua. */
    List<Interaccion> findByNegocioIdAndClienteIdOrderByOcurridoEnDesc(UUID negocioId,
            UUID clienteId);

    /**
     * Criterio 3: seguimientos con fecha cumplida que todavia no avisaron,
     * bloqueados para este barrido. {@code SKIP_LOCKED} (hint -2) deja que
     * varias instancias barran a la vez sin pelearse por las mismas filas.
     */
    @Query("select i from Interaccion i where i.seguimientoEn is not null"
            + " and i.seguimientoEn <= :hoy and i.seguimientoNotificadoEn is null"
            + " order by i.seguimientoEn")
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
    List<Interaccion> tomarSeguimientosVencidos(@Param("hoy") LocalDate hoy, Pageable lote);
}
