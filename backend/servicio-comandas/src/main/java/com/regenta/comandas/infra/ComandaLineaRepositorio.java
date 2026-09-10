package com.regenta.comandas.infra;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.regenta.comandas.domain.ComandaLinea;

public interface ComandaLineaRepositorio extends JpaRepository<ComandaLinea, UUID> {

    List<ComandaLinea> findByComandaIdOrderByLineaAsc(UUID comandaId);
}
