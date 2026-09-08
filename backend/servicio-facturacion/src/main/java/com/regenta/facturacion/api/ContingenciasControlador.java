package com.regenta.facturacion.api;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.regenta.comun.negocio.RequierePermiso;
import com.regenta.facturacion.aplicacion.ContingenciaDelNegocio;
import com.regenta.facturacion.aplicacion.GestionDeContingencia;
import com.regenta.facturacion.aplicacion.GestionDeFirmaYTransmision;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/** Contingencia de la DIAN. HU-057. */
@RestController
@RequestMapping("/api/facturacion/contingencias")
@Tag(name = "Contingencias", description = "Seguir facturando cuando la DIAN no responde")
public class ContingenciasControlador {

    private final GestionDeContingencia contingencias;
    private final GestionDeFirmaYTransmision firma;

    public ContingenciasControlador(GestionDeContingencia contingencias,
            GestionDeFirmaYTransmision firma) {
        this.contingencias = contingencias;
        this.firma = firma;
    }

    @GetMapping
    @Operation(summary = "Las contingencias del negocio, con las facturas afectadas de cada una")
    public List<ContingenciaDelNegocio> listar() {
        return contingencias.listar();
    }

    @GetMapping("/{contingenciaId}")
    @Operation(summary = "Una contingencia: inicio, fin, motivo y cuántas facturas quedaron afectadas")
    @ApiResponse(responseCode = "404", description = "No existe en este negocio")
    public ContingenciaDelNegocio ver(@PathVariable UUID contingenciaId) {
        return contingencias.ver(contingenciaId);
    }

    @PostMapping("/{contingenciaId}/cierre")
    @RequierePermiso("FACTURACION_FACTURA_CREAR")
    @Operation(summary = "Cierra la contingencia y retransmite en orden las facturas pendientes")
    @ApiResponse(responseCode = "404", description = "No existe en este negocio")
    public ContingenciaDelNegocio cerrar(@PathVariable UUID contingenciaId) {
        return firma.cerrarContingenciaYRetransmitir(contingenciaId);
    }
}
