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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.regenta.recursos.aplicacion.ConsumoDeServicioRegistrado;
import com.regenta.recursos.aplicacion.CotizacionDeServicio;
import com.regenta.recursos.aplicacion.GestionDeServiciosAdicionales;
import com.regenta.recursos.aplicacion.ServicioAdicionalDelNegocio;
import com.regenta.recursos.aplicacion.SolicitudDeConsumoDeServicio;
import com.regenta.recursos.aplicacion.SolicitudDeServicioAdicional;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** Servicios adicionales del negocio: desayuno, parqueadero, alquiler. HU-068. */
@RestController
@RequestMapping("/api/recursos/servicios")
@Tag(name = "Servicios adicionales", description = "Extras que se cobran sobre una reserva")
public class ServiciosAdicionalesControlador {

    private final GestionDeServiciosAdicionales servicios;

    public ServiciosAdicionalesControlador(GestionDeServiciosAdicionales servicios) {
        this.servicios = servicios;
    }

    @GetMapping
    @Operation(summary = "Los servicios del negocio; con ?soloActivos=true, solo los ofrecibles")
    public List<ServicioAdicionalDelNegocio> listar(
            @RequestParam(name = "soloActivos", defaultValue = "false") boolean soloActivos) {
        return servicios.listar(soloActivos);
    }

    @GetMapping("/{servicioId}")
    @Operation(summary = "Un servicio por id")
    public ServicioAdicionalDelNegocio ver(@PathVariable UUID servicioId) {
        return servicios.ver(servicioId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Da de alta un servicio adicional")
    @ApiResponse(responseCode = "409", description = "Ya hay un servicio con ese código")
    public ServicioAdicionalDelNegocio crear(@Valid @RequestBody SolicitudDeServicioAdicional s) {
        return servicios.crear(s);
    }

    @PutMapping("/{servicioId}")
    @Operation(summary = "Cambia un servicio adicional")
    public ServicioAdicionalDelNegocio actualizar(@PathVariable UUID servicioId,
            @Valid @RequestBody SolicitudDeServicioAdicional s) {
        return servicios.actualizar(servicioId, s);
    }

    @DeleteMapping("/{servicioId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Desactiva un servicio adicional")
    public void desactivar(@PathVariable UUID servicioId) {
        servicios.desactivar(servicioId);
    }

    @PostMapping("/{servicioId}/cotizacion")
    @Operation(summary = "Cuántas unidades y cuánto suma el servicio para una reserva")
    public CotizacionDeServicio cotizar(@PathVariable UUID servicioId,
            @Valid @RequestBody SolicitudDeConsumoDeServicio solicitud) {
        return servicios.cotizar(servicioId, solicitud);
    }

    @PostMapping("/{servicioId}/consumo")
    @Operation(summary = "Registra el consumo; si el servicio tiene producto, descuenta inventario")
    public ConsumoDeServicioRegistrado consumir(@PathVariable UUID servicioId,
            @Valid @RequestBody SolicitudDeConsumoDeServicio solicitud) {
        return servicios.consumir(servicioId, solicitud);
    }
}
