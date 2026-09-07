package com.regenta.facturacion.api;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.regenta.facturacion.aplicacion.GestionDeResoluciones;
import com.regenta.facturacion.aplicacion.ResolucionDelNegocio;
import com.regenta.facturacion.aplicacion.SolicitudDeResolucion;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** Resoluciones de numeración DIAN. HU-052. */
@RestController
@RequestMapping("/api/facturacion/resoluciones")
@Tag(name = "Resoluciones", description = "Los rangos de numeración autorizados por la DIAN")
public class ResolucionesControlador {

    private final GestionDeResoluciones resoluciones;

    public ResolucionesControlador(GestionDeResoluciones resoluciones) {
        this.resoluciones = resoluciones;
    }

    @GetMapping
    @Operation(summary = "Las resoluciones del negocio, de la que vence más tarde a la que vence antes")
    public List<ResolucionDelNegocio> listar() {
        return resoluciones.listar();
    }

    @GetMapping("/vigente")
    @Operation(summary = "La resolución vigente para un tipo de documento y sucursal")
    @ApiResponse(responseCode = "404", description = "No hay resolución vigente para ese tipo")
    @ApiResponse(responseCode = "422", description = "La resolución vigente ya venció")
    public ResolucionDelNegocio vigente(@RequestParam String tipoDocumento,
            @RequestParam(required = false) UUID sucursalId) {
        return resoluciones.verVigente(tipoDocumento, sucursalId);
    }

    @GetMapping("/{resolucionId}")
    @Operation(summary = "Una resolución por id")
    @ApiResponse(responseCode = "404", description = "No existe en este negocio")
    public ResolucionDelNegocio ver(@PathVariable UUID resolucionId) {
        return resoluciones.ver(resolucionId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Carga una resolución con su rango, clave técnica y vigencia")
    @ApiResponse(responseCode = "409", description = "Número repetido, o ya hay una vigente de ese tipo y sucursal")
    @ApiResponse(responseCode = "422", description = "Rango o vigencia inválidos")
    public ResolucionDelNegocio cargar(@Valid @RequestBody SolicitudDeResolucion solicitud) {
        return resoluciones.cargar(solicitud);
    }

    @DeleteMapping("/{resolucionId}")
    @Operation(summary = "Anula una resolución; libera el lugar de la vigente de ese tipo y sucursal")
    @ApiResponse(responseCode = "404", description = "No existe en este negocio")
    public ResolucionDelNegocio anular(@PathVariable UUID resolucionId) {
        return resoluciones.anular(resolucionId);
    }
}
