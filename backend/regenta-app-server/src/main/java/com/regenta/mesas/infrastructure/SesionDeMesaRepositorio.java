package com.regenta.mesas.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.regenta.mesas.domain.EstadoDeSesion;
import com.regenta.mesas.domain.SesionDeMesa;

public interface SesionDeMesaRepositorio extends JpaRepository<SesionDeMesa, UUID> {

    Optional<SesionDeMesa> findByIdAndNegocioId(UUID id, UUID negocioId);

    /** La sesiÃ³n viva de una mesa, si la hay (HU-082 criterio 2). */
    Optional<SesionDeMesa> findFirstByMesaPrincipalIdAndEstadoInOrderByAbiertaEnDesc(
            UUID mesaPrincipalId, List<EstadoDeSesion> estados);

    boolean existsByMesaPrincipalIdAndEstadoIn(UUID mesaPrincipalId, List<EstadoDeSesion> estados);

    /** Para el consumidor de comandas cerradas: la sesiÃ³n viva de una comanda. */
    Optional<SesionDeMesa> findFirstByComandaIdAndEstadoInOrderByAbiertaEnDesc(
            UUID comandaId, List<EstadoDeSesion> estados);

    List<SesionDeMesa> findByMesaPrincipalIdAndNegocioIdOrderByAbiertaEnDesc(
            UUID mesaPrincipalId, UUID negocioId);
}
