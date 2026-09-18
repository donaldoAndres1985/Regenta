package com.regenta.auditoria.api;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;

import com.regenta.auditoria.aplicacion.ConflictoDetalle;
import com.regenta.auditoria.aplicacion.GestionDeConflictos;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/** Conflictos de sincronización. HU-103. */
@RestController
@RequestMapping("/api/auditoria/conflictos")
@Tag(name = "Conflictos de sincronización", description = "Detección y resolución de conflictos offline")
public class ConflictosControlador {

    private final GestionDeConflictos gestion;

    public ConflictosControlador(GestionDeConflictos gestion) {
        this.gestion = gestion;
    }

    @GetMapping
    @Operation(summary = "Los conflictos sin resolver, con la versión del servidor y la del cliente")
    public List<ConflictoDetalle> pendientes() {
        return gestion.pendientes();
    }

    @PostMapping("/{id}/resolver")
    @Operation(summary = "Resuelve un conflicto eligiendo una versión")
    public void resolver(@PathVariable UUID id, @RequestBody Map<String, String> cuerpo) {
        gestion.resolver(id, cuerpo.get("resolucion"));
    }

    @PostMapping("/barrido")
    @Operation(summary = "Publica conflicto_sync_vencido por cada conflicto sin resolver hace más de un día")
    public Map<String, Integer> barrerSinResolver() {
        return Map.of("conflictosPublicados", gestion.barrerSinResolver());
    }
}
