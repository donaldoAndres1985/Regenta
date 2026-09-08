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

    /** Alertas todavía abiertas de una entidad y un tipo (HU-093 criterio 4). */
    @Query("""
            select a from Alerta a
            where a.negocioId = :negocioId and a.tipoCodigo = :tipoCodigo
              and a.entidadTipo = :entidadTipo and a.entidadId = :entidadId
              and a.estado in (com.regenta.alertas.domain.EstadoAlerta.NUEVA,
                               com.regenta.alertas.domain.EstadoAlerta.VISTA,
                               com.regenta.alertas.domain.EstadoAlerta.EN_CURSO)
            """)
    List<Alerta> activasDeEntidad(@Param("negocioId") UUID negocioId,
            @Param("tipoCodigo") String tipoCodigo, @Param("entidadTipo") String entidadTipo,
            @Param("entidadId") UUID entidadId);

    /**
     * El centro de alertas del usuario (HU-094 criterio 2): las alertas que le
     * llegaron por alguna entrega, las nuevas primero y por severidad.
     */
    @Query("""
            select a from Alerta a
            where a.negocioId = :negocioId
              and a.id in (select e.alertaId from Entrega e where e.usuarioId = :usuarioId)
            order by
              case a.estado when com.regenta.alertas.domain.EstadoAlerta.NUEVA then 0
                            when com.regenta.alertas.domain.EstadoAlerta.VISTA then 1
                            when com.regenta.alertas.domain.EstadoAlerta.EN_CURSO then 2
                            else 3 end,
              case a.severidad when com.regenta.alertas.domain.Severidad.CRITICA then 0
                               when com.regenta.alertas.domain.Severidad.ALTA then 1
                               when com.regenta.alertas.domain.Severidad.MEDIA then 2
                               else 3 end,
              a.generadaEn desc
            """)
    List<Alerta> delUsuario(@Param("negocioId") UUID negocioId,
            @Param("usuarioId") UUID usuarioId);
}
