package com.regenta.reportes.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.regenta.reportes.aplicacion.PanelDiario;
import com.regenta.reportes.aplicacion.ResumenDiario;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/** El panel de inicio del dueño del negocio. HU-097. */
@RestController
@RequestMapping("/api/reportes/panel")
@Tag(name = "Panel de inicio", description = "Agregados diarios y del mes, sin recorrer las tablas de hechos")
public class PanelControlador {

    private final PanelDiario panel;

    public PanelControlador(PanelDiario panel) {
        this.panel = panel;
    }

    @GetMapping("/hoy")
    @Operation(summary = "Lo vendido (o reservado, o pedido) hoy, en la zona horaria del negocio")
    public ResumenDiario hoy() {
        return panel.hoy();
    }

    @GetMapping("/mes")
    @Operation(summary = "Lo acumulado del mes en curso hasta hoy")
    public ResumenDiario mes() {
        return panel.mesActual();
    }
}
