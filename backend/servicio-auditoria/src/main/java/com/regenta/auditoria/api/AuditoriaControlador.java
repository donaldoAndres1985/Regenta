package com.regenta.auditoria.api;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.regenta.auditoria.aplicacion.ConsultaDeAuditoria;
import com.regenta.auditoria.aplicacion.EventoDeAuditoria;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/** La bitácora de auditoría. HU-101. */
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
}
