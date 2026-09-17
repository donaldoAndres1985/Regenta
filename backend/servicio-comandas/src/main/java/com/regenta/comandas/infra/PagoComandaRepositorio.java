package com.regenta.comandas.infra;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.regenta.comandas.domain.PagoComanda;

public interface PagoComandaRepositorio extends JpaRepository<PagoComanda, UUID> {

    List<PagoComanda> findByComandaIdOrderByRecibidoEnAsc(UUID comandaId);
}
