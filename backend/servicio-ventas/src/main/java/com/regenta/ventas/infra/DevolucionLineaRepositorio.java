package com.regenta.ventas.infra;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.regenta.ventas.domain.DevolucionLinea;

public interface DevolucionLineaRepositorio extends JpaRepository<DevolucionLinea, UUID> {
}
