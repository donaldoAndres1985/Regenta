package com.regenta.mesas.api;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.regenta.mesas.aplicacion.GestionDeMesas;
import com.regenta.mesas.aplicacion.MesaDelNegocio;
import com.regenta.mesas.aplicacion.PlanoDelSalon;
import com.regenta.mesas.aplicacion.SolicitudDeMesa;
import com.regenta.mesas.aplicacion.SolicitudDePosicion;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** Mesas del salón y su posición en el plano. HU-081. */
@RestController
@RequestMapping("/api/mesas")
@Tag(name = "Mesas", description = "Mesas del salón, su capacidad y su lugar en el plano")
public class MesasControlador {

    private final GestionDeMesas mesas;

    public MesasControlador(GestionDeMesas mesas) {
        this.mesas = mesas;
    }

    @GetMapping
    @Operation(summary = "Las mesas del negocio; se puede filtrar por zona")
    public List<MesaDelNegocio> listar(@RequestParam(required = false) UUID zonaId) {
        return mesas.listar(zonaId);
    }

    @GetMapping("/plano")
    @Operation(summary = "El plano del salón: zonas en orden con sus mesas")
    public PlanoDelSalon plano() {
        return mesas.plano();
    }

    @GetMapping("/{mesaId}")
    @Operation(summary = "Una mesa por id")
    public MesaDelNegocio ver(@PathVariable UUID mesaId) {
        return mesas.ver(mesaId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crea una mesa con su código, capacidad, zona y posición")
    @ApiResponse(responseCode = "409", description = "Ya hay una mesa con ese código")
    public MesaDelNegocio crear(@Valid @RequestBody SolicitudDeMesa solicitud) {
        return mesas.crear(solicitud);
    }

    @PutMapping("/{mesaId}")
    @Operation(summary = "Cambia la zona, el nombre, la capacidad o la forma de una mesa")
    public MesaDelNegocio actualizar(@PathVariable UUID mesaId,
            @Valid @RequestBody SolicitudDeMesa solicitud) {
        return mesas.actualizar(mesaId, solicitud);
    }

    @PutMapping("/{mesaId}/posicion")
    @Operation(summary = "Guarda la posición y el tamaño de la mesa en el plano")
    public MesaDelNegocio mover(@PathVariable UUID mesaId,
            @RequestBody SolicitudDePosicion solicitud) {
        return mesas.mover(mesaId, solicitud);
    }

    @DeleteMapping("/{mesaId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Borra una mesa (soft-delete)")
    @ApiResponse(responseCode = "409", description = "La mesa tiene una sesión abierta")
    public void eliminar(@PathVariable UUID mesaId) {
        mesas.eliminar(mesaId);
    }
}
