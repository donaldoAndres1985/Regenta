package com.regenta.inventario.infra;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.regenta.inventario.domain.Traslado;

public interface TrasladoRepositorio extends JpaRepository<Traslado, UUID> {

    boolean existsByNegocioIdAndNumero(UUID negocioId, String numero);
}
