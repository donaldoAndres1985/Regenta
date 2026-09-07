package com.regenta.clientes.api;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.regenta.clientes.aplicacion.GestionDeInteracciones;
import com.regenta.clientes.aplicacion.InteraccionDelCliente;
import com.regenta.clientes.aplicacion.SolicitudDeInteraccion;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** Historial de interacciones del cliente. HU-024. */
@RestController
@RequestMapping("/api/clientes/{clienteId}/interacciones")
@Tag(name = "Interacciones", description = "Llamadas, visitas y notas sobre un cliente")
public class InteraccionesControlador {

    private final GestionDeInteracciones interacciones;

    public InteraccionesControlador(GestionDeInteracciones interacciones) {
        this.interacciones = interacciones;
    }

    @GetMapping
    @Operation(summary = "Las interacciones del cliente, de la mas reciente a la mas antigua")
    @ApiResponse(responseCode = "404", description = "El cliente no existe en este negocio")
    public List<InteraccionDelCliente> listar(@PathVariable UUID clienteId) {
        return interacciones.listar(clienteId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registra una interaccion con tipo, fecha, autor y detalle")
    @ApiResponse(responseCode = "404", description = "El cliente no existe en este negocio")
    @ApiResponse(responseCode = "422", description = "Tipo de interaccion no valido")
    public InteraccionDelCliente registrar(@PathVariable UUID clienteId,
            @Valid @RequestBody SolicitudDeInteraccion solicitud) {
        return interacciones.registrar(clienteId, solicitud);
    }
}
