package com.regenta.usuarios.infra;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.regenta.usuarios.domain.ClaveNegocioModulo;
import com.regenta.usuarios.domain.NegocioModulo;

public interface NegocioModuloRepositorio extends JpaRepository<NegocioModulo, ClaveNegocioModulo> {

    List<NegocioModulo> findByClaveNegocioId(UUID negocioId);

    List<NegocioModulo> findByClaveNegocioIdAndActivoTrue(UUID negocioId);

    Optional<NegocioModulo> findByClaveNegocioIdAndClaveModuloCodigo(UUID negocioId, String modulo);
}
