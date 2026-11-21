package com.regenta.compras.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.regenta.compras.application.ConfiguracionDeComprasDelNegocio;
import com.regenta.compras.application.GestionDeConfiguracionDeCompras;
import com.regenta.compras.application.SolicitudDeConfiguracionDeCompras;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** HU-173 · Cómo compra el negocio: aprobación de órdenes y tolerancia de recepción. */
@RestController
@RequestMapping("/api/compras/configuracion")
@Tag(name = "Configuración de compras", description = "Aprobación de órdenes y tolerancia de recepción")
public class ConfiguracionDeComprasControlador {

    private final GestionDeConfiguracionDeCompras configuracion;

    public ConfiguracionDeComprasControlador(GestionDeConfiguracionDeCompras configuracion) {
        this.configuracion = configuracion;
    }

    @GetMapping
    @Operation(summary = "La configuración de compras del negocio (o la de por omisión)")
    public ConfiguracionDeComprasDelNegocio ver() {
        return configuracion.ver();
    }

    @PutMapping
    @Operation(summary = "Cambia si las órdenes exigen aprobación, desde qué monto, y la tolerancia")
    @ApiResponse(responseCode = "403", description = "Falta COMPRAS_CONFIGURAR")
    @ApiResponse(responseCode = "422", description = "Tolerancia fuera de 0 a 100, o monto mínimo no positivo")
    public ConfiguracionDeComprasDelNegocio cambiar(@Valid @RequestBody SolicitudDeConfiguracionDeCompras solicitud) {
        return configuracion.cambiar(solicitud);
    }
}
