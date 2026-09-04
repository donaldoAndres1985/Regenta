package com.regenta.usuarios.infra;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.regenta.usuarios.domain.Plan;

/** Catalogo global: sin RLS, igual para todos los negocios. */
public interface PlanRepositorio extends JpaRepository<Plan, UUID> {

    Optional<Plan> findByCodigo(String codigo);

    List<Plan> findByActivoTrueOrderByOrden();
}
