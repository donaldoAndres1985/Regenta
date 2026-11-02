package com.regenta.recursos.web;

import com.regenta.recursos.api.*;

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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.regenta.recursos.application.AplicacionDeCancelacion;
import com.regenta.recursos.application.GestionDePoliticasDeCancelacion;
import com.regenta.recursos.application.PoliticaCancelacionDelNegocio;
import com.regenta.recursos.application.SolicitudDeAplicacionDeCancelacion;
import com.regenta.recursos.application.SolicitudDePoliticaCancelacion;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** PolÃ­ticas de cancelaciÃ³n del negocio. HU-068. */
@RestController
@RequestMapping("/api/recursos/politicas-cancelacion")
@Tag(name = "PolÃ­ticas de cancelaciÃ³n", description = "Anticipo requerido y penalizaciÃ³n por cancelar")
public class PoliticasDeCancelacionControlador {

    private final GestionDePoliticasDeCancelacion politicas;

    public PoliticasDeCancelacionControlador(GestionDePoliticasDeCancelacion politicas) {
        this.politicas = politicas;
    }

    @GetMapping
    @Operation(summary = "Las polÃ­ticas de cancelaciÃ³n del negocio")
    public List<PoliticaCancelacionDelNegocio> listar() {
        return politicas.listar();
    }

    @GetMapping("/default")
    @Operation(summary = "La polÃ­tica por defecto del negocio")
    @ApiResponse(responseCode = "404", description = "El negocio no tiene una por defecto")
    public PoliticaCancelacionDelNegocio verDefault() {
        return politicas.verDefault();
    }

    @GetMapping("/{politicaId}")
    @Operation(summary = "Una polÃ­tica por id")
    public PoliticaCancelacionDelNegocio ver(@PathVariable UUID politicaId) {
        return politicas.ver(politicaId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crea una polÃ­tica de cancelaciÃ³n")
    public PoliticaCancelacionDelNegocio crear(@Valid @RequestBody SolicitudDePoliticaCancelacion s) {
        return politicas.crear(s);
    }

    @PutMapping("/{politicaId}")
    @Operation(summary = "Cambia una polÃ­tica de cancelaciÃ³n")
    public PoliticaCancelacionDelNegocio actualizar(@PathVariable UUID politicaId,
            @Valid @RequestBody SolicitudDePoliticaCancelacion s) {
        return politicas.actualizar(politicaId, s);
    }

    @PostMapping("/{politicaId}/por-defecto")
    @Operation(summary = "Marca esta polÃ­tica como la de por defecto (desmarca la anterior)")
    public PoliticaCancelacionDelNegocio marcarPorDefecto(@PathVariable UUID politicaId) {
        return politicas.marcarPorDefecto(politicaId);
    }

    @DeleteMapping("/{politicaId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Desactiva una polÃ­tica de cancelaciÃ³n")
    public void desactivar(@PathVariable UUID politicaId) {
        politicas.desactivar(politicaId);
    }

    @PostMapping("/aplicacion")
    @Operation(summary = "Aplica una polÃ­tica a una reserva: anticipo requerido y penalizaciÃ³n")
    public AplicacionDeCancelacion aplicar(
            @Valid @RequestBody SolicitudDeAplicacionDeCancelacion solicitud) {
        return politicas.aplicar(solicitud);
    }
}
