package com.regenta.alertas.infra;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import com.regenta.alertas.domain.Alerta;

public interface AlertaRepositorio extends JpaRepository<Alerta, UUID> {

    Optional<Alerta> findByIdAndNegocioId(UUID id, UUID negocioId);

    List<Alerta> findByNegocioIdOrderByGeneradaEnDesc(UUID negocioId);

    /**
     * Toma la alerta de una huella con bloqueo, para decidir sin carrera si se
     * crea, se deja o se reabre (HU-092 criterio 3).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Alerta a where a.negocioId = :negocioId and a.huella = :huella")
    Optional<Alerta> tomarPorHuella(@Param("negocioId") UUID negocioId,
            @Param("huella") String huella);
}
