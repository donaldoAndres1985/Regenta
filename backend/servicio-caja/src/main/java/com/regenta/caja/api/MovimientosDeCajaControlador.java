package com.regenta.caja.api;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.regenta.caja.aplicacion.GestionDeConfigDeCaja;
import com.regenta.caja.aplicacion.GestionDeConfigDeCaja.ConfigCajaDelNegocio;
import com.regenta.caja.aplicacion.GestionDeMovimientosManuales;
import com.regenta.caja.aplicacion.MovimientoDelNegocio;
import com.regenta.caja.aplicacion.SolicitudDeConfigCaja;
import com.regenta.caja.aplicacion.SolicitudDeIngreso;
import com.regenta.caja.aplicacion.SolicitudDeRetiro;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** Ingresos, retiros y gastos de caja + su configuración. HU-061. */
@RestController
@RequestMapping("/api/caja")
@Tag(name = "Movimientos manuales de caja", description = "Ingresos, retiros y gastos del turno")
public class MovimientosDeCajaControlador {

    private final GestionDeMovimientosManuales manuales;
    private final GestionDeConfigDeCaja config;

    public MovimientosDeCajaControlador(GestionDeMovimientosManuales manuales,
            GestionDeConfigDeCaja config) {
        this.manuales = manuales;
        this.config = config;
    }

    @PostMapping("/sesiones/{sesionId}/ingresos")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registra un ingreso de efectivo; sube el efectivo esperado")
    public MovimientoDelNegocio ingreso(@PathVariable UUID sesionId,
            @Valid @RequestBody SolicitudDeIngreso solicitud) {
        return manuales.registrarIngreso(sesionId, solicitud);
    }

    @PostMapping("/sesiones/{sesionId}/retiros")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registra un retiro o gasto; baja el efectivo esperado")
    @ApiResponse(responseCode = "422", description = "Sobre el umbral y sin autorización, o sin concepto")
    public MovimientoDelNegocio retiro(@PathVariable UUID sesionId,
            @Valid @RequestBody SolicitudDeRetiro solicitud) {
        return manuales.registrarRetiro(sesionId, solicitud);
    }

    @GetMapping("/config")
    @Operation(summary = "La configuración de caja del negocio")
    public ConfigCajaDelNegocio verConfig() {
        return config.ver();
    }

    @PutMapping("/config")
    @Operation(summary = "Fija el umbral de retiro que exige autorización")
    public ConfigCajaDelNegocio fijarConfig(@Valid @RequestBody SolicitudDeConfigCaja solicitud) {
        return config.fijar(solicitud);
    }
}
