package com.regenta.recursos.infra;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.regenta.recursos.domain.Tarifa;

public interface TarifaRepositorio extends JpaRepository<Tarifa, UUID> {

    Optional<Tarifa> findByIdAndNegocioId(UUID id, UUID negocioId);

    List<Tarifa> findByNegocioIdOrderByPrioridadDescNombreAsc(UUID negocioId);

    /** Las tarifas candidatas para un recurso: las suyas y las de su tipo. */
    @Query("""
            select t from Tarifa t
            where t.negocioId = :negocioId and t.activa = true
              and (t.recursoId = :recursoId or t.tipoRecursoId = :tipoRecursoId)
            order by t.prioridad desc
            """)
    List<Tarifa> candidatasPara(@Param("negocioId") UUID negocioId,
            @Param("recursoId") UUID recursoId, @Param("tipoRecursoId") UUID tipoRecursoId);
}
