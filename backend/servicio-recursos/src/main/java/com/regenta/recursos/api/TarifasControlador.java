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

import com.regenta.recursos.aplicacion.CotizacionDeEstadia;
import com.regenta.recursos.aplicacion.CotizadorDeEstadia;
import com.regenta.recursos.aplicacion.GestionDeTarifas;
import com.regenta.recursos.aplicacion.SolicitudDeCotizacion;
import com.regenta.recursos.aplicacion.SolicitudDeTarifa;
import com.regenta.recursos.aplicacion.TarifaDelNegocio;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** Tarifas por temporada, día y franja con prioridad. HU-066. */
@RestController
@RequestMapping("/api/recursos")
@Tag(name = "Tarifas", description = "Cobrar distinto un martes de mayo que un sábado de diciembre")
public class TarifasControlador {

    private final GestionDeTarifas tarifas;
    private final CotizadorDeEstadia cotizador;

    public TarifasControlador(GestionDeTarifas tarifas, CotizadorDeEstadia cotizador) {
        this.tarifas = tarifas;
        this.cotizador = cotizador;
    }

    @GetMapping("/tarifas")
    @Operation(summary = "Las tarifas del negocio, la de mayor prioridad primero")
    public List<TarifaDelNegocio> listar() {
        return tarifas.listar();
    }

    @GetMapping("/tarifas/{tarifaId}")
    @Operation(summary = "Una tarifa por id")
    public TarifaDelNegocio ver(@PathVariable UUID tarifaId) {
        return tarifas.ver(tarifaId);
    }

    @PostMapping("/tarifas")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Define una tarifa para un tipo de recurso o un recurso puntual")
    public TarifaDelNegocio crear(@Valid @RequestBody SolicitudDeTarifa solicitud) {
        return tarifas.crear(solicitud);
    }

    @PutMapping("/tarifas/{tarifaId}")
    @Operation(summary = "Cambia una tarifa")
    public TarifaDelNegocio actualizar(@PathVariable UUID tarifaId,
            @Valid @RequestBody SolicitudDeTarifa solicitud) {
        return tarifas.actualizar(tarifaId, solicitud);
    }

    @DeleteMapping("/tarifas/{tarifaId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Desactiva una tarifa")
    public void desactivar(@PathVariable UUID tarifaId) {
        tarifas.desactivar(tarifaId);
    }

    @PostMapping("/cotizaciones")
    @Operation(summary = "Cotiza una estancia noche por noche, cada una a su tarifa")
    public CotizacionDeEstadia cotizar(@Valid @RequestBody SolicitudDeCotizacion solicitud) {
        return cotizador.cotizar(solicitud);
    }
}
