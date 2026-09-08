package com.regenta.recursos.infra;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.regenta.recursos.domain.PoliticaCancelacion;

public interface PoliticaCancelacionRepositorio extends JpaRepository<PoliticaCancelacion, UUID> {

    Optional<PoliticaCancelacion> findByIdAndNegocioId(UUID id, UUID negocioId);

    List<PoliticaCancelacion> findByNegocioIdOrderByNombreAsc(UUID negocioId);

    Optional<PoliticaCancelacion> findFirstByNegocioIdAndEsDefaultTrue(UUID negocioId);

    /** Quita el default a todas las del negocio menos a {@code exceptoId}. Una sola por negocio. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update PoliticaCancelacion p set p.esDefault = false "
            + "where p.negocioId = :negocioId and p.esDefault = true and p.id <> :exceptoId")
    void quitarDefaultSalvo(@Param("negocioId") UUID negocioId, @Param("exceptoId") UUID exceptoId);
}
