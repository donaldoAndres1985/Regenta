package com.regenta.usuarios.api;

import java.util.Optional;
import java.util.UUID;

/**
 * El plan contratado de un negocio (BASICO, PROFESIONAL, EMPRESARIAL), para lo que otros módulos
 * deciden según el plan. HU-131: cuánto se conserva la bitácora si el negocio no lo configuró.
 */
public interface PlanDelNegocioApi {

    /** El código del plan del negocio; vacío si el negocio no existe. */
    Optional<String> planDe(UUID negocioId);
}
