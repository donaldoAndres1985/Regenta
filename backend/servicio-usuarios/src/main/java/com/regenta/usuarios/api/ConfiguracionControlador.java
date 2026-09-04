package com.regenta.usuarios.api;

import java.math.BigDecimal;
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

import com.regenta.usuarios.aplicacion.CambioDeConfiguracion;
import com.regenta.usuarios.aplicacion.ConfiguracionVigente;
import com.regenta.usuarios.aplicacion.GestionDeConfiguracion;
import com.regenta.usuarios.aplicacion.ImpuestoDelNegocio;
import com.regenta.usuarios.aplicacion.SolicitudDeImpuesto;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** Configuracion fiscal y de operacion. HU-018. */
@RestController
@RequestMapping("/api/usuarios")
@Tag(name = "Configuracion", description = "Datos fiscales, impuestos y preferencias")
public class ConfiguracionControlador {

    private final GestionDeConfiguracion gestion;

    public ConfiguracionControlador(GestionDeConfiguracion gestion) {
        this.gestion = gestion;
    }

    @GetMapping("/configuracion")
    @Operation(summary = "La configuracion del negocio")
    public ConfiguracionVigente ver() {
        return gestion.ver();
    }

    @PutMapping("/configuracion")
    @Operation(summary = "Guarda datos fiscales, de contacto y preferencias")
    public ConfiguracionVigente actualizar(@Valid @RequestBody CambioDeConfiguracion cambio) {
        return gestion.actualizar(cambio);
    }

    @GetMapping("/impuestos")
    @Operation(summary = "Los impuestos del negocio")
    public List<ImpuestoDelNegocio> impuestos() {
        return gestion.impuestos();
    }

    @PostMapping("/impuestos")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crea un impuesto")
    public ImpuestoDelNegocio crear(@Valid @RequestBody SolicitudDeImpuesto solicitud) {
        return gestion.crearImpuesto(solicitud);
    }

    @PutMapping("/impuestos/{impuestoId}")
    @Operation(summary = "Corrige el nombre de un impuesto")
    @ApiResponse(responseCode = "422", description = "El porcentaje no se cambia: crea otro")
    public ImpuestoDelNegocio renombrar(@PathVariable UUID impuestoId,
            @RequestParam String nombre,
            @RequestParam(required = false) BigDecimal porcentaje) {
        return gestion.renombrarImpuesto(impuestoId, nombre, porcentaje);
    }

    @PostMapping("/impuestos/{impuestoId}/por-defecto")
    @Operation(summary = "Marca el impuesto que tomaran los productos nuevos")
    public ImpuestoDelNegocio porDefecto(@PathVariable UUID impuestoId) {
        return gestion.marcarPorDefecto(impuestoId);
    }

    @DeleteMapping("/impuestos/{impuestoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Desactiva un impuesto. No se borra: hay documentos que lo usan")
    public void desactivar(@PathVariable UUID impuestoId) {
        gestion.desactivarImpuesto(impuestoId);
    }
}
