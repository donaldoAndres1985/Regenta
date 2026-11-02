package com.regenta.shared.security;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletResponse;

public final class RespuestaDeError {

    private static final ObjectMapper json = new ObjectMapper();

    private RespuestaDeError() {}

    public static void escribir(HttpServletResponse respuesta, HttpStatus estado, String detalle, String traceId)
            throws IOException {
        respuesta.setStatus(estado.value());
        respuesta.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        respuesta.setCharacterEncoding(StandardCharsets.UTF_8.name());

        var cuerpo = new java.util.LinkedHashMap<String, Object>();
        cuerpo.put("type", "about:blank");
        cuerpo.put("title", estado.getReasonPhrase());
        cuerpo.put("status", estado.value());
        cuerpo.put("detail", detalle);
        if (traceId != null && !traceId.isBlank()) {
            cuerpo.put("trace_id", traceId);
        }

        respuesta.getWriter().write(json.writeValueAsString(cuerpo));
    }
}
