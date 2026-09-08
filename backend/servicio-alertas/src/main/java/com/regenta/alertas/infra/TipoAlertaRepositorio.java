package com.regenta.alertas.infra;

import org.springframework.data.jpa.repository.JpaRepository;

import com.regenta.alertas.domain.TipoAlerta;

public interface TipoAlertaRepositorio extends JpaRepository<TipoAlerta, String> {
}
