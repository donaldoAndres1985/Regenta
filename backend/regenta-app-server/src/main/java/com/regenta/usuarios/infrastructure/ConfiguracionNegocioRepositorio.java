package com.regenta.usuarios.infrastructure;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.regenta.usuarios.domain.ConfiguracionNegocio;

public interface ConfiguracionNegocioRepositorio
        extends JpaRepository<ConfiguracionNegocio, UUID> {
}
