package com.regenta.inventario.infra;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.regenta.inventario.domain.AjusteLinea;

public interface AjusteLineaRepositorio extends JpaRepository<AjusteLinea, UUID> {

    List<AjusteLinea> findByAjusteIdOrderById(UUID ajusteId);
}
