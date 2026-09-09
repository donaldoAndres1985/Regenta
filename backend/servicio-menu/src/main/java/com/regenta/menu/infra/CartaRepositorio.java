package com.regenta.menu.infra;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.regenta.menu.domain.Carta;

public interface CartaRepositorio extends JpaRepository<Carta, UUID> {

    Optional<Carta> findByIdAndNegocioId(UUID id, UUID negocioId);

    List<Carta> findByNegocioIdOrderByEsDefaultDescNombreAsc(UUID negocioId);

    List<Carta> findByNegocioIdAndActivaTrueOrderByEsDefaultDescNombreAsc(UUID negocioId);

    /** Deja como default solo a {@code exceptoId}: una carta por defecto por negocio. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Carta c set c.esDefault = false "
            + "where c.negocioId = :negocioId and c.esDefault = true and c.id <> :exceptoId")
    void quitarDefaultSalvo(@Param("negocioId") UUID negocioId, @Param("exceptoId") UUID exceptoId);
}
