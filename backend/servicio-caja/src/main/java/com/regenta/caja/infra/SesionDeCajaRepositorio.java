package com.regenta.caja.infra;

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
