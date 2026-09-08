package com.regenta.recursos.api;

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

import com.regenta.recursos.aplicacion.AplicacionDeCancelacion;
import com.regenta.recursos.aplicacion.GestionDePoliticasDeCancelacion;
import com.regenta.recursos.aplicacion.PoliticaCancelacionDelNegocio;
import com.regenta.recursos.aplicacion.SolicitudDeAplicacionDeCancelacion;
import com.regenta.recursos.aplicacion.SolicitudDePoliticaCancelacion;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** Políticas de cancelación del negocio. HU-068. */
@RestController
@RequestMapping("/api/recursos/politicas-cancelacion")
@Tag(name = "Políticas de cancelación", description = "Anticipo requerido y penalización por cancelar")
public class PoliticasDeCancelacionControlador {

    private final GestionDePoliticasDeCancelacion politicas;

    public PoliticasDeCancelacionControlador(GestionDePoliticasDeCancelacion politicas) {
        this.politicas = politicas;
    }

    @GetMapping
    @Operation(summary = "Las políticas de cancelación del negocio")
    public List<PoliticaCancelacionDelNegocio> listar() {
        return politicas.listar();
    }

    @GetMapping("/default")
    @Operation(summary = "La política por defecto del negocio")
    @ApiResponse(responseCode = "404", description = "El negocio no tiene una por defecto")
    public PoliticaCancelacionDelNegocio verDefault() {
        return politicas.verDefault();
    }

    @GetMapping("/{politicaId}")
    @Operation(summary = "Una política por id")
    public PoliticaCancelacionDelNegocio ver(@PathVariable UUID politicaId) {
        return politicas.ver(politicaId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crea una política de cancelación")
    public PoliticaCancelacionDelNegocio crear(@Valid @RequestBody SolicitudDePoliticaCancelacion s) {
        return politicas.crear(s);
    }

    @PutMapping("/{politicaId}")
    @Operation(summary = "Cambia una política de cancelación")
    public PoliticaCancelacionDelNegocio actualizar(@PathVariable UUID politicaId,
            @Valid @RequestBody SolicitudDePoliticaCancelacion s) {
        return politicas.actualizar(politicaId, s);
    }

    @PostMapping("/{politicaId}/por-defecto")
    @Operation(summary = "Marca esta política como la de por defecto (desmarca la anterior)")
    public PoliticaCancelacionDelNegocio marcarPorDefecto(@PathVariable UUID politicaId) {
        return politicas.marcarPorDefecto(politicaId);
    }

    @DeleteMapping("/{politicaId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Desactiva una política de cancelación")
    public void desactivar(@PathVariable UUID politicaId) {
        politicas.desactivar(politicaId);
    }

    @PostMapping("/aplicacion")
    @Operation(summary = "Aplica una política a una reserva: anticipo requerido y penalización")
    public AplicacionDeCancelacion aplicar(
            @Valid @RequestBody SolicitudDeAplicacionDeCancelacion solicitud) {
        return politicas.aplicar(solicitud);
    }
}
