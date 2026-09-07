package com.regenta.inventario.api;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.regenta.inventario.aplicacion.AjusteDelNegocio;
import com.regenta.inventario.aplicacion.GestionDeAjustes;
import com.regenta.inventario.aplicacion.SolicitudDeAjuste;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** Ajustes de inventario: conteo físico, merma, avería. HU-033. */
@RestController
@RequestMapping("/api/inventario/ajustes")
@Tag(name = "Ajustes", description = "Corregir el stock con constancia de por qué cambió")
public class AjustesControlador {

    private final GestionDeAjustes ajustes;

    public AjustesControlador(GestionDeAjustes ajustes) {
        this.ajustes = ajustes;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Carga un ajuste en BORRADOR; la diferencia la calcula la base")
    @ApiResponse(responseCode = "409", description = "Ya hay un ajuste con ese número")
    public AjusteDelNegocio crear(@Valid @RequestBody SolicitudDeAjuste solicitud) {
        return ajustes.crear(solicitud);
    }

    @PostMapping("/{ajusteId}/aplicacion")
    @Operation(summary = "Aplica el ajuste: genera los movimientos y lo cierra")
    @ApiResponse(responseCode = "409", description = "El ajuste no está en BORRADOR")
    @ApiResponse(responseCode = "422", description = "El ajuste no tiene motivo")
    public AjusteDelNegocio aplicar(@PathVariable UUID ajusteId) {
        return ajustes.aplicar(ajusteId);
    }

    @GetMapping("/{ajusteId}")
    @Operation(summary = "Un ajuste con sus líneas")
    public AjusteDelNegocio ver(@PathVariable UUID ajusteId) {
        return ajustes.ver(ajusteId);
    }
}
