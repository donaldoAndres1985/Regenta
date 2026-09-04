package com.regenta.comun.negocio;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.core.Ordered;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Traduce las cabeceras del gateway al contexto de la peticion.
 *
 * <p>Si no viene negocio, no se inventa ninguno: la peticion sigue sin contexto
 * y cualquier consulta a una tabla con RLS devolvera cero filas. Falla cerrado.
 */
public class FiltroDeNegocio extends OncePerRequestFilter implements Ordered {

    @Override
    protected void doFilterInternal(HttpServletRequest peticion, HttpServletResponse respuesta,
            FilterChain cadena) throws ServletException, IOException {
        try {
            uuid(peticion.getHeader(CabecerasDeNegocio.NEGOCIO))
                    .map(negocio -> leer(negocio, peticion))
                    .ifPresent(ContextoDeNegocio::establecer);
            cadena.doFilter(peticion, respuesta);
        } finally {
            ContextoDeNegocio.limpiar();
        }
    }

    private static DatosDelNegocio leer(UUID negocio, HttpServletRequest peticion) {
        return new DatosDelNegocio(
                negocio,
                uuid(peticion.getHeader(CabecerasDeNegocio.USUARIO)).orElse(null),
                texto(peticion.getHeader(CabecerasDeNegocio.PLAN)),
                texto(peticion.getHeader(CabecerasDeNegocio.PATRON)),
                lista(peticion.getHeader(CabecerasDeNegocio.ROLES)),
                lista(peticion.getHeader(CabecerasDeNegocio.MODULOS)),
                lista(peticion.getHeader(CabecerasDeNegocio.PERMISOS)),
                listaDeUuid(peticion.getHeader(CabecerasDeNegocio.SUCURSALES)));
    }

    private static Optional<UUID> uuid(String valor) {
        if (valor == null || valor.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(valor.trim()));
        } catch (IllegalArgumentException noEsUuid) {
            return Optional.empty();
        }
    }

    private static String texto(String valor) {
        return valor == null ? "" : valor.trim();
    }

    private static Set<String> lista(String valor) {
        if (valor == null || valor.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(valor.split(","))
                .map(String::trim)
                .filter(elemento -> !elemento.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static Set<UUID> listaDeUuid(String valor) {
        Set<UUID> sucursales = new LinkedHashSet<>();
        for (String elemento : lista(valor)) {
            uuid(elemento).ifPresent(sucursales::add);
        }
        return sucursales;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }
}
