package com.regenta.compras.infra;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.regenta.compras.domain.RecepcionLinea;

public interface RecepcionLineaRepositorio extends JpaRepository<RecepcionLinea, UUID> {

    List<RecepcionLinea> findByRecepcionId(UUID recepcionId);
}
