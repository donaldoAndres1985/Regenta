package com.regenta.caja.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.regenta.caja.domain.EstadoSesionCaja;
import com.regenta.caja.domain.SesionDeCaja;

public interface SesionDeCajaRepositorio extends JpaRepository<SesionDeCaja, UUID> {

    Optional<SesionDeCaja> findByIdAndNegocioId(UUID id, UUID negocioId);

    Optional<SesionDeCaja> findFirstByNegocioIdAndCajaIdAndEstado(UUID negocioId, UUID cajaId,
            EstadoSesionCaja estado);

    List<SesionDeCaja> findByNegocioIdOrderByAbiertaEnDesc(UUID negocioId);

    /** HU-146 · la sesión abierta más reciente que abrió ese usuario. */
    Optional<SesionDeCaja> findFirstByNegocioIdAndUsuarioAperturaIdAndEstadoOrderByAbiertaEnDesc(
            UUID negocioId, UUID usuarioAperturaId, EstadoSesionCaja estado);

    /**
     * HU-143 C1 · Los cierres (sesiones ya cerradas) de un rango por fecha de cierre, opcionalmente de un
     * cajero —el que abrió o el que cerró—, el más reciente primero y paginados.
     */
    @org.springframework.data.jpa.repository.Query(value = """
            select s from SesionDeCaja s
            where s.negocioId = :negocioId
              and s.estado <> com.regenta.caja.domain.EstadoSesionCaja.ABIERTA
              and s.cerradaEn >= :desde and s.cerradaEn < :hasta
              and (:cajeroId is null or s.usuarioAperturaId = :cajeroId or s.usuarioCierreId = :cajeroId)
            order by s.cerradaEn desc, s.numero desc
            """, countQuery = """
            select count(s) from SesionDeCaja s
            where s.negocioId = :negocioId
              and s.estado <> com.regenta.caja.domain.EstadoSesionCaja.ABIERTA
              and s.cerradaEn >= :desde and s.cerradaEn < :hasta
              and (:cajeroId is null or s.usuarioAperturaId = :cajeroId or s.usuarioCierreId = :cajeroId)
            """)
    org.springframework.data.domain.Page<SesionDeCaja> cierres(
            @org.springframework.data.repository.query.Param("negocioId") UUID negocioId,
            @org.springframework.data.repository.query.Param("desde") java.time.OffsetDateTime desde,
            @org.springframework.data.repository.query.Param("hasta") java.time.OffsetDateTime hasta,
            @org.springframework.data.repository.query.Param("cajeroId") UUID cajeroId,
            org.springframework.data.domain.Pageable pagina);

    /** HU-143 · Quienes han abierto caja en el negocio, para filtrar los cierres por cajero. */
    @org.springframework.data.jpa.repository.Query("""
            select distinct s.usuarioAperturaId from SesionDeCaja s
            where s.negocioId = :negocioId and s.usuarioAperturaId is not null
            """)
    List<UUID> cajerosDelNegocio(@org.springframework.data.repository.query.Param("negocioId") UUID negocioId);

    /** Sesiones en un rango de fechas, con filtro opcional de estado y de caja (HU-063). */
    @org.springframework.data.jpa.repository.Query("""
            select s from SesionDeCaja s
            where s.negocioId = :negocioId
              and s.abiertaEn >= :desde and s.abiertaEn < :hasta
              and (:estado is null or s.estado = :estado)
              and (:cajaId is null or s.cajaId = :cajaId)
            order by s.abiertaEn desc
            """)
    List<SesionDeCaja> enRango(
            @org.springframework.data.repository.query.Param("negocioId") UUID negocioId,
            @org.springframework.data.repository.query.Param("desde") java.time.OffsetDateTime desde,
            @org.springframework.data.repository.query.Param("hasta") java.time.OffsetDateTime hasta,
            @org.springframework.data.repository.query.Param("estado") EstadoSesionCaja estado,
            @org.springframework.data.repository.query.Param("cajaId") UUID cajaId);
}
