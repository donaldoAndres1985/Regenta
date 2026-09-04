package com.regenta.usuarios.aplicacion;

import java.util.List;
import java.util.UUID;

/**
 * O el token, o la lista de negocios para elegir. Nunca las dos cosas.
 *
 * <p>Un correo puede trabajar en dos negocios clientes de Regenta. Cuando pasa,
 * el API no adivina: devuelve la lista y espera a que la app diga en cual.
 */
public record ResultadoDeLogin(
        boolean debeElegirNegocio,
        List<NegocioParaElegir> negocios,
        String tokenDeAcceso,
        String tokenDeRefresco,
        long expiraEnSegundos,
        UUID negocioId,
        UUID usuarioId,
        String plan,
        String patron,
        List<String> roles,
        List<String> modulos) {

    public static ResultadoDeLogin elija(List<NegocioParaElegir> negocios) {
        return new ResultadoDeLogin(true, negocios, null, null, 0, null, null, null, null,
                List.of(), List.of());
    }

    public static ResultadoDeLogin conToken(String acceso, String refresco, long expiraEn,
            UUID negocioId, UUID usuarioId, String plan, String patron, List<String> roles,
            List<String> modulos) {
        return new ResultadoDeLogin(false, List.of(), acceso, refresco, expiraEn, negocioId,
                usuarioId, plan, patron, roles, modulos);
    }
}
