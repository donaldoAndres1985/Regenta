package com.regenta.alertas.infra;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.regenta.alertas.domain.Entrega;

public interface EntregaRepositorio extends JpaRepository<Entrega, UUID> {

    List<Entrega> findByAlertaId(UUID alertaId);
}
