package com.regenta.clientes.infra;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.regenta.clientes.domain.Recaudo;

public interface RecaudoRepositorio extends JpaRepository<Recaudo, UUID> {
}
