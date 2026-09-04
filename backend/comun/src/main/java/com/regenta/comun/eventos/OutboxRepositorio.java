package com.regenta.comun.eventos;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;

public interface OutboxRepositorio extends JpaRepository<OutboxEvento, UUID> {

    /**
     * Los pendientes mas viejos, bloqueados para este publicador.
     *
     * <p>El bloqueo con salto —{@code FOR UPDATE SKIP LOCKED}— es lo que permite tener
     * varias instancias del servicio publicando a la vez: cada una toma un lote distinto
     * en vez de pelearse por las mismas filas o publicar el mismo evento dos veces. El
     * valor -2 del hint es {@code SKIP_LOCKED}; sin el, la segunda instancia esperaria.
     *
     * <p>En JPQL y no en SQL nativo a proposito: el nombre del esquema sale del mapeo de
     * la entidad. Una consulta nativa se ejecuta contra el search_path de la conexion
     * —public— y no encontraria la tabla, que vive en el esquema del servicio.
     */
    @Query("select e from OutboxEvento e where e.estado = 'PENDIENTE' order by e.creadoEn")
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
    List<OutboxEvento> tomarPendientes(Pageable lote);

    long countByEstado(String estado);
}
