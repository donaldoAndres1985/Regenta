package com.regenta.usuarios.infra;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.regenta.usuarios.domain.ConfiguracionNegocio;

public interface ConfiguracionNegocioRepositorio
        extends JpaRepository<ConfiguracionNegocio, UUID> {
}
