package com.regenta.auditoria.api;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.regenta.auditoria.aplicacion.ConsultaDeAuditoria;
import com.regenta.auditoria.aplicacion.EventoDeAuditoria;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/** La bitácora de auditoría. HU-101, HU-105. */
@RestController
@RequestMapping("/api/auditoria/eventos")
@Tag(name = "Auditoría", description = "Bitácora append-only de quién cambió qué y cuándo")
public class AuditoriaControlador {

    private final ConsultaDeAuditoria consulta;

    public AuditoriaControlador(ConsultaDeAuditoria consulta) {
        this.consulta = consulta;
    }

    @GetMapping
    @Operation(summary = "Reconstruye todo lo que hizo un request, aunque cruzara varios servicios")
    public List<EventoDeAuditoria> porTraceId(@RequestParam String traceId) {
        return consulta.porTraceId(traceId);
    }

    @GetMapping("/por-entidad")
    @Operation(summary = "El historial de un documento (una venta, un producto...), con autor y fecha")
    public List<EventoDeAuditoria> porEntidad(@RequestParam String entidadTipo,
            @RequestParam UUID entidadId) {
        return consulta.porEntidad(entidadTipo, entidadId);
    }

    @GetMapping("/por-usuario")
    @Operation(summary = "Toda la actividad de un usuario en un rango de fechas")
    public List<EventoDeAuditoria> porUsuario(@RequestParam UUID usuarioId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime hasta) {
        return consulta.porUsuario(usuarioId, desde, hasta);
    }
}
