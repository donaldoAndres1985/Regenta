package com.regenta.usuarios.aplicacion;

import java.util.List;
import java.util.UUID;

/**
 * Lo que la app necesita saber de su negocio para pintar la navegacion. HU-020.
 *
 * <p>Ojo: esconder una opcion es cortesia, no seguridad. El backend valida
 * igual con {@code @RequiereModulo} y {@code @RequierePermiso}.
 */
public record ResumenDelNegocio(
        UUID negocioId,
        String nombreComercial,
        String plan,
        String patronOperativo,
        String estado,
        Integer maxUsuarios,
        int maxSucursales,
        long usuariosActivos,
        List<ModuloActivo> modulos) {
}
