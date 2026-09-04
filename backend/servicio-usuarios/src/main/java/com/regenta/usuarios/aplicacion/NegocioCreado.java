package com.regenta.usuarios.aplicacion;

import java.util.List;
import java.util.UUID;

/** Lo que queda despues del alta, que es lo que el operador necesita ver. */
public record NegocioCreado(
        UUID negocioId,
        String nombreComercial,
        String plan,
        String patronOperativo,
        String estado,
        UUID administradorId,
        String administradorEmail,
        UUID sucursalPrincipalId,
        List<String> modulosActivos,
        List<String> roles) {
}
