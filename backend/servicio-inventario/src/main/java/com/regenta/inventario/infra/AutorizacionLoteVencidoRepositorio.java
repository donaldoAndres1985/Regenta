package com.regenta.inventario.infra;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.regenta.inventario.domain.AutorizacionLoteVencido;

public interface AutorizacionLoteVencidoRepositorio
        extends JpaRepository<AutorizacionLoteVencido, UUID> {

    List<AutorizacionLoteVencido> findByLoteIdOrderByAutorizadoEnAsc(UUID loteId);
}
