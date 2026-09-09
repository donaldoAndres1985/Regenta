package com.regenta.mesas.api;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.regenta.mesas.aplicacion.GestionDeSesionesDeMesa;
import com.regenta.mesas.aplicacion.SesionDelNegocio;
import com.regenta.mesas.aplicacion.SolicitudDeApertura;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** Sesiones de mesa: abrir, ocupar, pedir cuenta, cerrar y limpiar. HU-082. */
@RestController
@RequestMapping("/api/mesas")
@Tag(name = "Sesiones de mesa", description = "La ocupación de una mesa, de que llegan a que se van")
public class SesionesDeMesaControlador {

    private final GestionDeSesionesDeMesa sesiones;

    public SesionesDeMesaControlador(GestionDeSesionesDeMesa sesiones) {
        this.sesiones = sesiones;
    }

    @PostMapping("/{mesaId}/sesiones")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Abre la mesa con el número de comensales")
    @ApiResponse(responseCode = "409", description = "La mesa ya tiene una sesión abierta")
    public SesionDelNegocio abrir(@PathVariable UUID mesaId,
            @Valid @RequestBody SolicitudDeApertura solicitud) {
        return sesiones.abrir(mesaId, solicitud);
    }

    @GetMapping("/{mesaId}/sesion")
    @Operation(summary = "La sesión abierta de la mesa")
    public SesionDelNegocio actual(@PathVariable UUID mesaId) {
        return sesiones.sesionActual(mesaId);
    }

    @GetMapping("/{mesaId}/sesiones")
    @Operation(summary = "El historial de sesiones de la mesa, la más reciente primero")
    public List<SesionDelNegocio> historial(@PathVariable UUID mesaId) {
        return sesiones.historial(mesaId);
    }

    @GetMapping("/sesiones/{sesionId}")
    @Operation(summary = "Una sesión: cuánto duró y cuántos comensales tuvo")
    public SesionDelNegocio ver(@PathVariable UUID sesionId) {
        return sesiones.ver(sesionId);
    }

    @PostMapping("/sesiones/{sesionId}/mesas/{mesaId}")
    @Operation(summary = "Une una mesa libre a la sesión (grupo grande)")
    @ApiResponse(responseCode = "409", description = "Esa mesa ya está ocupada")
    public SesionDelNegocio unir(@PathVariable UUID sesionId, @PathVariable UUID mesaId) {
        return sesiones.unir(sesionId, mesaId);
    }

    @DeleteMapping("/sesiones/{sesionId}/mesas/{mesaId}")
    @Operation(summary = "Saca una mesa unida del grupo; queda por limpiar")
    public SesionDelNegocio separar(@PathVariable UUID sesionId, @PathVariable UUID mesaId) {
        return sesiones.separar(sesionId, mesaId);
    }

    @PostMapping("/sesiones/{sesionId}/cuenta")
    @Operation(summary = "Marca que se pidió la cuenta")
    public SesionDelNegocio pedirCuenta(@PathVariable UUID sesionId) {
        return sesiones.pedirCuenta(sesionId);
    }

    @PostMapping("/sesiones/{sesionId}/cierre")
    @Operation(summary = "Cierra la sesión; la mesa queda por limpiar")
    public SesionDelNegocio cerrar(@PathVariable UUID sesionId) {
        return sesiones.cerrar(sesionId);
    }

    @PostMapping("/{mesaId}/limpieza")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Marca la mesa limpia: pasa de «por limpiar» a libre")
    public void marcarLimpia(@PathVariable UUID mesaId) {
        sesiones.marcarLimpia(mesaId);
    }
}
