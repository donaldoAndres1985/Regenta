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
}
