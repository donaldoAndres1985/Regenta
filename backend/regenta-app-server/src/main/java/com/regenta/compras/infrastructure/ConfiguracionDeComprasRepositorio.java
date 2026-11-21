package com.regenta.compras.infrastructure;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.regenta.compras.domain.ConfiguracionDeCompras;

public interface ConfiguracionDeComprasRepositorio extends JpaRepository<ConfiguracionDeCompras, UUID> {
}
