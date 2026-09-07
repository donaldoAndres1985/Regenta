package com.regenta.inventario.infra;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.regenta.inventario.domain.Ajuste;

public interface AjusteRepositorio extends JpaRepository<Ajuste, UUID> {

    boolean existsByNegocioIdAndNumero(UUID negocioId, String numero);
}
