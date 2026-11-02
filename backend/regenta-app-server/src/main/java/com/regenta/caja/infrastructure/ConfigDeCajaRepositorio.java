package com.regenta.caja.infrastructure;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.regenta.caja.domain.ConfigDeCaja;

public interface ConfigDeCajaRepositorio extends JpaRepository<ConfigDeCaja, UUID> {
}
