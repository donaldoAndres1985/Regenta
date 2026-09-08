package com.regenta.caja.api;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import com.regenta.caja.aplicacion.GestionDeArqueo;
import com.regenta.caja.aplicacion.GestionDeSesionesDeCaja;
import com.regenta.caja.aplicacion.MovimientoDelNegocio;
import com.regenta.caja.aplicacion.RegistroDeMovimientos;
import com.regenta.caja.aplicacion.ResumenDeArqueo;
import com.regenta.caja.aplicacion.SesionDelNegocio;
import com.regenta.caja.aplicacion.SolicitudDeApertura;
import com.regenta.caja.aplicacion.SolicitudDeArqueo;
import com.regenta.caja.aplicacion.SolicitudDeCierre;

import org.springframework.web.bind.annotation.PutMapping;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** Apertura, arqueo y cierre de sesiones de caja. HU-059. */
@RestController
@RequestMapping("/api/caja/sesiones")
@Tag(name = "Sesiones de caja", description = "De quién es la responsabilidad del efectivo del turno")
public class SesionesDeCajaControlador {

    private final GestionDeSesionesDeCaja sesiones;
    private final RegistroDeMovimientos movimientos;
    private final GestionDeArqueo arqueo;

    public SesionesDeCajaControlador(GestionDeSesionesDeCaja sesiones,
            RegistroDeMovimientos movimientos, GestionDeArqueo arqueo) {
        this.sesiones = sesiones;
        this.movimientos = movimientos;
        this.arqueo = arqueo;
    }

    @GetMapping("/activa")
    @Operation(summary = "La sesión abierta de una caja, si la hay")
    @ApiResponse(responseCode = "404", description = "La caja no tiene una sesión abierta")
    public SesionDelNegocio activa(@RequestParam UUID cajaId) {
        return sesiones.activaDe(cajaId);
    }

    @GetMapping("/{sesionId}")
    @Operation(summary = "Una sesión de caja con su arqueo")
    public SesionDelNegocio ver(@PathVariable UUID sesionId) {
        return sesiones.ver(sesionId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Abre una sesión de caja con su base")
    @ApiResponse(responseCode = "409", description = "La caja ya tiene una sesión abierta")
    public SesionDelNegocio abrir(@Valid @RequestBody SolicitudDeApertura solicitud) {
        return sesiones.abrir(solicitud);
    }

    @GetMapping("/{sesionId}/movimientos")
    @Operation(summary = "Los movimientos de la sesión, en orden")
    public List<MovimientoDelNegocio> movimientos(@PathVariable UUID sesionId) {
        return movimientos.deLaSesion(sesionId);
    }

    @GetMapping("/{sesionId}/arqueo")
    @Operation(summary = "El conteo por denominaciones de la sesión, con su total y diferencia")
    public ResumenDeArqueo verArqueo(@PathVariable UUID sesionId) {
        return arqueo.ver(sesionId);
    }

    @PutMapping("/{sesionId}/arqueo")
    @Operation(summary = "Guarda el conteo por denominaciones; devuelve total y diferencia al momento")
    @ApiResponse(responseCode = "409", description = "Denominación repetida")
    public ResumenDeArqueo guardarArqueo(@PathVariable UUID sesionId,
            @Valid @RequestBody SolicitudDeArqueo solicitud) {
        return arqueo.guardar(sesionId, solicitud);
    }

    @PostMapping("/{sesionId}/cierre")
    @Operation(summary = "Cierra la sesión declarando lo contado; calcula la diferencia")
    @ApiResponse(responseCode = "409", description = "La sesión ya está cerrada")
    public SesionDelNegocio cerrar(@PathVariable UUID sesionId,
            @Valid @RequestBody SolicitudDeCierre solicitud) {
        return sesiones.cerrar(sesionId, solicitud);
    }
}
