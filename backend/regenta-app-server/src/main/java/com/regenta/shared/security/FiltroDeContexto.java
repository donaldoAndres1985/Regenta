package com.regenta.shared.security;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import com.regenta.shared.tenancy.CabecerasDeNegocio;
import com.regenta.shared.tenancy.ContextoDeNegocio;
import com.regenta.shared.tenancy.DatosDelNegocio;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Extrae la identidad y el contexto de negocio directamente del JWT verificado
 * (o de las cabeceras internas si aplica) y puebla el ContextoDeNegocio.
 */
public class FiltroDeContexto extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(FiltroDeContexto.class);
    private static final Set<String> SIN_SERVICIO = Set.of("SUSPENDIDO", "CANCELADO");

    @Override
    protected void doFilterInternal(HttpServletRequest peticion, HttpServletResponse respuesta, FilterChain cadena)
            throws ServletException, IOException {
        String traceId = Trazas.de(peticion);
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth instanceof JwtAuthenticationToken jwtAuth) {
                Jwt jwt = jwtAuth.getToken();
                String estado = texto(jwt.getClaim("estado_negocio")).toUpperCase(Locale.ROOT);
                if (SIN_SERVICIO.contains(estado)) {
                    log.warn("402 en seguridad: negocio {} en estado {} ruta={} trace_id={}",
                            texto(jwt.getClaim("negocio_id")), estado, peticion.getRequestURI(), traceId);
                    RespuestaDeError.escribir(respuesta, HttpStatus.PAYMENT_REQUIRED,
                            "negocio " + estado.toLowerCase(Locale.ROOT), traceId);
                    return;
                }

                String negocioStr = texto(jwt.getClaim("negocio_id"));
                if (!negocioStr.isBlank()) {
                    try {
                        UUID negocioId = UUID.fromString(negocioStr);
                        UUID usuarioId = null;
                        String sub = jwt.getSubject();
                        if (sub != null && !sub.isBlank()) {
                            try { usuarioId = UUID.fromString(sub.trim()); } catch (Exception ignored) {}
                        }

                        DatosDelNegocio datos = new DatosDelNegocio(
                                negocioId,
                                usuarioId,
                                texto(jwt.getClaim("plan")),
                                texto(jwt.getClaim("patron")),
                                aSet(jwt.getClaim("roles")),
                                aSet(jwt.getClaim("modulos")),
                                aSet(jwt.getClaim("permisos")),
                                aSetUuid(jwt.getClaim("sucursales"))
                        );
                        ContextoDeNegocio.establecer(datos);
                    } catch (IllegalArgumentException e) {
                        log.warn("401 en seguridad: UUID de negocio inválido {} trace_id={}", negocioStr, traceId);
                        RespuestaDeError.escribir(respuesta, HttpStatus.UNAUTHORIZED, "token con negocio inválido", traceId);
                        return;
                    }
                }
            } else {
                // Fallback a cabeceras si se envían directamente (por ejemplo en tests)
                String headerNegocio = peticion.getHeader(CabecerasDeNegocio.NEGOCIO);
                if (headerNegocio != null && !headerNegocio.isBlank()) {
                    try {
                        UUID negocioId = UUID.fromString(headerNegocio.trim());
                        UUID usuarioId = null;
                        String headerUsuario = peticion.getHeader(CabecerasDeNegocio.USUARIO);
                        if (headerUsuario != null && !headerUsuario.isBlank()) {
                            try { usuarioId = UUID.fromString(headerUsuario.trim()); } catch (Exception ignored) {}
                        }
                        DatosDelNegocio datos = new DatosDelNegocio(
                                negocioId,
                                usuarioId,
                                texto(peticion.getHeader(CabecerasDeNegocio.PLAN)),
                                texto(peticion.getHeader(CabecerasDeNegocio.PATRON)),
                                aSet(peticion.getHeader(CabecerasDeNegocio.ROLES)),
                                aSet(peticion.getHeader(CabecerasDeNegocio.MODULOS)),
                                aSet(peticion.getHeader(CabecerasDeNegocio.PERMISOS)),
                                aSetUuid(peticion.getHeader(CabecerasDeNegocio.SUCURSALES))
                        );
                        ContextoDeNegocio.establecer(datos);
                    } catch (Exception ignored) {}
                }
            }

            cadena.doFilter(peticion, respuesta);
        } finally {
            ContextoDeNegocio.limpiar();
        }
    }

    private static String texto(Object valor) {
        return valor == null ? "" : valor.toString().trim();
    }

    private static Set<String> aSet(Object valor) {
        if (valor == null) {
            return Set.of();
        }
        if (valor instanceof Collection<?> col) {
            return col.stream().map(FiltroDeContexto::texto)
                    .filter(s -> !s.isBlank())
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        }
        String str = valor.toString().trim();
        if (str.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(str.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static Set<UUID> aSetUuid(Object valor) {
        Set<UUID> res = new LinkedHashSet<>();
        for (String item : aSet(valor)) {
            try {
                res.add(UUID.fromString(item));
            } catch (Exception ignored) {}
        }
        return res;
    }

}
