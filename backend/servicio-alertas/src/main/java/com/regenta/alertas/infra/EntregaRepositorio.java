package com.regenta.alertas.infra;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.regenta.alertas.domain.Entrega;

public interface EntregaRepositorio extends JpaRepository<Entrega, UUID> {

    List<Entrega> findByAlertaId(UUID alertaId);

    List<Entrega> findByAlertaIdAndUsuarioId(UUID alertaId, UUID usuarioId);

    /**
     * La cola de reintentos (HU-094 criterio 3): pendiente/fallida cuya hora ya
     * llegó. El {@code 5} es {@code Entrega.MAX_INTENTOS} (HQL no resuelve la
     * constante Java).
     */
    @Query("""
            select e from Entrega e
            where e.negocioId = :negocioId
              and e.estado in (com.regenta.alertas.domain.EstadoEntrega.PENDIENTE,
                               com.regenta.alertas.domain.EstadoEntrega.FALLIDA)
              and e.intentos < 5
              and (e.proximoIntento is null or e.proximoIntento <= :ahora)
            order by e.proximoIntento asc
            """)
    List<Entrega> paraReintentar(@Param("negocioId") UUID negocioId,
            @Param("ahora") OffsetDateTime ahora);
}
