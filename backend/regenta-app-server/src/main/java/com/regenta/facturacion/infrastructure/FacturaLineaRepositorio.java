package com.regenta.facturacion.infrastructure;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.regenta.facturacion.domain.FacturaLinea;

public interface FacturaLineaRepositorio extends JpaRepository<FacturaLinea, UUID> {

    List<FacturaLinea> findByFacturaIdOrderByLineaAsc(UUID facturaId);
}
