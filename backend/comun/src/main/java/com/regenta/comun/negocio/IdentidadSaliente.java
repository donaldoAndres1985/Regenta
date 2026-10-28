package com.regenta.comun.negocio;

import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.HttpHeaders;

/**
 * Con qué identidad llama un servicio a otro (HU-138).
 *
 * <p>Si hay un usuario detrás, se reenvía la suya tal cual: el servicio de
 * destino aplica sus permisos, igual que si la petición viniera del gateway.
 * Si no lo hay —un barrido, un listener de eventos—, va como {@code SISTEMA},
 * con el módulo de destino y solo el permiso de lectura que necesita esa
 * consulta. Es el mismo modelo de confianza del resto: solo el gateway queda
 * expuesto (decisión del 27-oct en #258).
 */
public final class IdentidadSaliente {

    public static final String SISTEMA = "SISTEMA";

    private IdentidadSaliente() {
    }

    public static void aplicar(HttpHeaders cabeceras, UUID negocioId, String moduloDestino,
            String permisoDeSistema) {
        DatosDelNegocio quien = ContextoDeNegocio.actual();
        cabeceras.set(CabecerasDeNegocio.NEGOCIO, negocioId.toString());
        cabeceras.set(CabecerasDeNegocio.PLAN, quien.plan() == null ? "" : quien.plan());
        cabeceras.set(CabecerasDeNegocio.PATRON, quien.patron() == null ? "" : quien.patron());
        if (quien.usuario() == null) {
            cabeceras.set(CabecerasDeNegocio.ROLES, SISTEMA);
            cabeceras.set(CabecerasDeNegocio.MODULOS, moduloDestino);
            cabeceras.set(CabecerasDeNegocio.PERMISOS, permisoDeSistema);
            return;
        }
        cabeceras.set(CabecerasDeNegocio.USUARIO, quien.usuario().toString());
        cabeceras.set(CabecerasDeNegocio.ROLES, String.join(",", quien.roles()));
        cabeceras.set(CabecerasDeNegocio.MODULOS, String.join(",", quien.modulos()));
        cabeceras.set(CabecerasDeNegocio.PERMISOS, String.join(",", quien.permisos()));
        if (!quien.sucursales().isEmpty()) {
            cabeceras.set(CabecerasDeNegocio.SUCURSALES, quien.sucursales().stream()
                    .map(UUID::toString).collect(Collectors.joining(",")));
        }
    }
}
