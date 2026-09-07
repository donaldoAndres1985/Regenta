package com.regenta.facturacion.infra;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.regenta.facturacion.domain.FacturaImpuesto;

public interface FacturaImpuestoRepositorio extends JpaRepository<FacturaImpuesto, UUID> {

    List<FacturaImpuesto> findByFacturaId(UUID facturaId);
}
