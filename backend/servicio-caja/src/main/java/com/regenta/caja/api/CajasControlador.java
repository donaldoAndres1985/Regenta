package com.regenta.caja.api;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.regenta.caja.aplicacion.CajaDelNegocio;
import com.regenta.caja.aplicacion.GestionDeCajas;
import com.regenta.caja.aplicacion.SolicitudDeCaja;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** Puntos de cobro del negocio. HU-059. */
@RestController
@RequestMapping("/api/caja/cajas")
@Tag(name = "Cajas", description = "Los puntos de cobro contra los que se abren sesiones")
public class CajasControlador {

    private final GestionDeCajas cajas;

    public CajasControlador(GestionDeCajas cajas) {
        this.cajas = cajas;
    }

    @GetMapping
    @Operation(summary = "Las cajas activas del negocio")
    public List<CajaDelNegocio> listar() {
        return cajas.listar();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registra un punto de cobro")
    @ApiResponse(responseCode = "409", description = "Ya hay una caja con ese código")
    public CajaDelNegocio crear(@Valid @RequestBody SolicitudDeCaja solicitud) {
        return cajas.crear(solicitud);
    }
}
