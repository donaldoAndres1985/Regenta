package com.regenta.gateway.seguridad;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

/**
 * Traduce el token a cabeceras y corta el paso a los negocios sin servicio.
 * HU-007, criterios 2 y 4.
 *
 * <p>Las cabeceras {@code X-Regenta-*} que traiga el cliente se descartan antes
 * de escribir las nuestras: el unico origen de la verdad es el token firmado.
 * Rio abajo cada servicio recibe ademas el {@code Authorization} original, para
 * que pueda revalidar por su cuenta en vez de confiar en el borde.
 */
@Component
public class FiltroDeContexto implements GlobalFilter, Ordered {

    public static final String NEGOCIO = "X-Regenta-Negocio";
    public static final String USUARIO = "X-Regenta-Usuario";
    public static final String PLAN = "X-Regenta-Plan";
    public static final String PATRON = "X-Regenta-Patron";
    public static final String ROLES = "X-Regenta-Roles";
    public static final String SUCURSALES = "X-Regenta-Sucursales";

    private static final List<String> CABECERAS_DE_CONTEXTO =
            List.of(NEGOCIO, USUARIO, PLAN, PATRON, ROLES, SUCURSALES);

    /** Estados en los que el negocio no recibe servicio hasta que se ponga al dia. */
    private static final Set<String> SIN_SERVICIO = Set.of("SUSPENDIDO", "CANCELADO");

    private static final Logger registro = LoggerFactory.getLogger(FiltroDeContexto.class);

    @Override
    public Mono<Void> filter(ServerWebExchange intercambio, GatewayFilterChain cadena) {
        return intercambio.getPrincipal()
                .filter(JwtAuthenticationToken.class::isInstance)
                .cast(JwtAuthenticationToken.class)
                .map(JwtAuthenticationToken::getToken)
                .map(Optional::of)
                .defaultIfEmpty(Optional.empty())
                .flatMap(token -> token
                        .map(jwt -> enrutar(intercambio, cadena, jwt))
                        .orElseGet(() -> RespuestaDeError.escribir(intercambio,
                                HttpStatus.UNAUTHORIZED, "token ausente o invalido")));
    }

    private Mono<Void> enrutar(ServerWebExchange intercambio, GatewayFilterChain cadena, Jwt jwt) {
        String estado = texto(jwt.getClaim("estado_negocio")).toUpperCase(Locale.ROOT);
        if (SIN_SERVICIO.contains(estado)) {
            registro.warn("402 en el gateway: negocio {} en estado {} ruta={} trace_id={}",
                    texto(jwt.getClaim("negocio_id")), estado,
                    intercambio.getRequest().getPath().value(), Trazas.de(intercambio));
            return RespuestaDeError.escribir(intercambio, HttpStatus.PAYMENT_REQUIRED,
                    "negocio " + estado.toLowerCase(Locale.ROOT));
        }

        String negocio = texto(jwt.getClaim("negocio_id"));
        if (negocio.isBlank()) {
            registro.warn("401 en el gateway: token sin negocio_id ruta={} trace_id={}",
                    intercambio.getRequest().getPath().value(), Trazas.de(intercambio));
            return RespuestaDeError.escribir(intercambio, HttpStatus.UNAUTHORIZED,
                    "token sin negocio");
        }

        ServerHttpRequest peticion = intercambio.getRequest().mutate()
                .headers(cabeceras -> {
                    for (String cabecera : CABECERAS_DE_CONTEXTO) {
                        cabeceras.remove(cabecera);
                    }
                    cabeceras.set(NEGOCIO, negocio);
                    poner(cabeceras, USUARIO, texto(jwt.getSubject()));
                    poner(cabeceras, PLAN, texto(jwt.getClaim("plan")));
                    poner(cabeceras, PATRON, texto(jwt.getClaim("patron")));
                    poner(cabeceras, ROLES, lista(jwt.getClaim("roles")));
                    poner(cabeceras, SUCURSALES, lista(jwt.getClaim("sucursales")));
                    cabeceras.set(Trazas.CABECERA, Trazas.de(intercambio));
                })
                .build();

        return cadena.filter(intercambio.mutate().request(peticion).build());
    }

    private static void poner(org.springframework.http.HttpHeaders cabeceras, String nombre, String valor) {
        if (!valor.isBlank()) {
            cabeceras.set(nombre, valor);
        }
    }

    private static String texto(Object valor) {
        return valor == null ? "" : valor.toString().trim();
    }

    private static String lista(Object valor) {
        if (valor instanceof Collection<?> elementos) {
            return elementos.stream().map(FiltroDeContexto::texto)
                    .filter(elemento -> !elemento.isBlank())
                    .collect(Collectors.joining(","));
        }
        return texto(valor);
    }

    @Override
    public int getOrder() {
        return 100;
    }
}
