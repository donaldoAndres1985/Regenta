package com.regenta.gateway.seguridad;

import java.nio.charset.StandardCharsets;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

/**
 * Cuerpo unico para todo rechazo del gateway. Deliberadamente escueto: el
 * gateway no explica reglas de negocio, solo dice que no y con que traza.
 */
final class RespuestaDeError {

    private RespuestaDeError() {
    }

    static Mono<Void> escribir(ServerWebExchange intercambio, HttpStatus estado, String motivo) {
        ServerHttpResponse respuesta = intercambio.getResponse();
        String traza = Trazas.de(intercambio);
        respuesta.setStatusCode(estado);
        respuesta.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        respuesta.getHeaders().set(Trazas.CABECERA, traza);
        String cuerpo = "{\"error\":\"" + motivo + "\",\"trace_id\":\"" + traza + "\"}";
        return respuesta.writeWith(
                Mono.just(respuesta.bufferFactory().wrap(cuerpo.getBytes(StandardCharsets.UTF_8))));
    }
}
