package com.regenta.auditoria.api;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.regenta.auditoria.aplicacion.DescargaIncremental;
import com.regenta.auditoria.aplicacion.ResultadoDeDescarga;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/** Descarga incremental de cambios del servidor. HU-104. */
@RestController
@RequestMapping("/api/auditoria/sincronizacion/cambios")
@Tag(name = "Descarga incremental", description = "Cambios del servidor desde el último cursor del dispositivo")
public class DescargaIncrementalControlador {

    private final DescargaIncremental descarga;

    public DescargaIncrementalControlador(DescargaIncremental descarga) {
        this.descarga = descarga;
    }

    @GetMapping
    @Operation(summary = "Cambios posteriores al cursor; fuerza descarga completa si el dispositivo "
            + "llevaba demasiado sin sincronizar")
    public ResultadoDeDescarga cambios(@RequestParam String dispositivo,
            @RequestParam(required = false) Long cursor) {
        return descarga.cambiosDesde(dispositivo, cursor);
    }

    @PostMapping("/confirmar")
    @Operation(summary = "Confirma que la descarga terminó: avanza el cursor del dispositivo")
    public void confirmar(@RequestBody Map<String, Object> cuerpo) {
        descarga.confirmarDescarga((String) cuerpo.get("dispositivo"),
                Long.parseLong(cuerpo.get("cursor").toString()));
    }
}
