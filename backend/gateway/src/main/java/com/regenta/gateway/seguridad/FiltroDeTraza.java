package com.regenta.gateway.seguridad;

import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import reactor.core.publisher.Mono;

/**
 * Primer filtro de la cadena: fija la traza antes que nada, para que hasta un
 * 401 de Spring Security salga con ella.
 */
@Component
public class FiltroDeTraza implements WebFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange intercambio, WebFilterChain cadena) {
        intercambio.getResponse().getHeaders().set(Trazas.CABECERA, Trazas.de(intercambio));
        return cadena.filter(intercambio);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
