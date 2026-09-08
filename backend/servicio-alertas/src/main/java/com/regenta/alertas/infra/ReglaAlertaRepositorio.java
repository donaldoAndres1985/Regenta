package com.regenta.alertas.infra;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.regenta.alertas.domain.ReglaAlerta;

public interface ReglaAlertaRepositorio extends JpaRepository<ReglaAlerta, UUID> {

    Optional<ReglaAlerta> findByIdAndNegocioId(UUID id, UUID negocioId);

    List<ReglaAlerta> findByNegocioIdOrderByNombreAsc(UUID negocioId);

    List<ReglaAlerta> findByNegocioIdAndTipoCodigoAndActivaTrue(UUID negocioId, String tipoCodigo);

    boolean existsByNegocioIdAndTipoCodigoAndNombre(UUID negocioId, String tipoCodigo,
            String nombre);
}
