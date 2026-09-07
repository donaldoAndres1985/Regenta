package com.regenta.ventas.api;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.regenta.ventas.aplicacion.CotizacionDelNegocio;
import com.regenta.ventas.aplicacion.GestionDeCotizaciones;
import com.regenta.ventas.aplicacion.ResultadoDeConversion;
import com.regenta.ventas.aplicacion.SolicitudDeCotizacion;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/** Cotizaciones. HU-044. */
@RestController
@RequestMapping("/api/cotizaciones")
@Tag(name = "Cotizaciones", description = "Cotizar y convertir en venta sin volver a digitar")
public class CotizacionesControlador {

    private final GestionDeCotizaciones cotizaciones;

    public CotizacionesControlador(GestionDeCotizaciones cotizaciones) {
        this.cotizaciones = cotizaciones;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crea una cotización con sus líneas")
    public CotizacionDelNegocio crear(@Valid @RequestBody SolicitudDeCotizacion solicitud) {
        return cotizaciones.crear(solicitud);
    }

    @PostMapping("/{cotizacionId}/conversion")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Convierte la cotización en una venta en borrador con las mismas líneas")
    @ApiResponse(responseCode = "409", description = "La cotización ya se convirtió")
    public ResultadoDeConversion convertir(@PathVariable UUID cotizacionId,
            @Valid @RequestBody Conversion cuerpo) {
        return cotizaciones.convertir(cotizacionId, cuerpo.bodegaId());
    }

    @GetMapping("/{cotizacionId}")
    @Operation(summary = "Una cotización; si se convirtió, trae el id de la venta")
    public CotizacionDelNegocio ver(@PathVariable UUID cotizacionId) {
        return cotizaciones.ver(cotizacionId);
    }

    /** Cuerpo de la conversión: la bodega de la venta resultante. */
    public record Conversion(@NotNull UUID bodegaId) {
    }
}
