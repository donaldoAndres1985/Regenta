package com.regenta.comun.eventos;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OutboxRepositorio extends JpaRepository<OutboxEvento, UUID> {

    /**
     * Los pendientes mas viejos, bloqueados para este publicador.
     *
     * <p>{@code FOR UPDATE SKIP LOCKED} es lo que permite tener varias instancias del
     * servicio publicando a la vez: cada una toma un lote distinto en vez de pelearse
     * por las mismas filas o publicar el mismo evento dos veces.
     */
    @Query(value = """
            SELECT * FROM outbox_eventos
            WHERE estado = 'PENDIENTE'
            ORDER BY creado_en
            LIMIT :limite
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<OutboxEvento> tomarPendientes(@Param("limite") int limite);

    long countByEstado(String estado);
}
