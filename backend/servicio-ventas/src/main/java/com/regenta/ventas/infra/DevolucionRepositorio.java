package com.regenta.ventas.infra;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.regenta.ventas.domain.Devolucion;

public interface DevolucionRepositorio extends JpaRepository<Devolucion, UUID> {
}
