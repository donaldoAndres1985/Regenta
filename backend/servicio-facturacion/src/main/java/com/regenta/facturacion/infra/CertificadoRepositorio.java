package com.regenta.facturacion.infra;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.regenta.facturacion.domain.Certificado;
import com.regenta.facturacion.domain.EstadoCertificado;

public interface CertificadoRepositorio extends JpaRepository<Certificado, UUID> {

    List<Certificado> findByNegocioIdOrderByVigenteHastaDesc(UUID negocioId);

    List<Certificado> findByNegocioIdAndEstado(UUID negocioId, EstadoCertificado estado);

    boolean existsByNegocioIdAndAlias(UUID negocioId, String alias);
}
