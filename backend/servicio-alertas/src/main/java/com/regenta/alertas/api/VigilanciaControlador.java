package com.regenta.alertas.api;

import java.util.Map;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.regenta.alertas.aplicacion.GestionDeVigilancia;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/** Vigilancia de inventario: barrido de lotes por vencer. HU-093. */
@RestController
@RequestMapping("/api/alertas/vigilancia")
@Tag(name = "Vigilancia de inventario", description = "Alertas de stock bajo y lotes por vencer")
public class VigilanciaControlador {

    private final GestionDeVigilancia vigilancia;

    public VigilanciaControlador(GestionDeVigilancia vigilancia) {
        this.vigilancia = vigilancia;
    }

    @PostMapping("/vencimientos")
    @Operation(summary = "Recorre los lotes por vencer del negocio y publica lote_por_vencer")
    public Map<String, Integer> barrerVencimientos() {
        return Map.of("lotesPublicados", vigilancia.barrerVencimientos());
    }
}
