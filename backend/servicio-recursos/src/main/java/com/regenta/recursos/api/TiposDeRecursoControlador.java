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

import com.regenta.recursos.aplicacion.AtributoDelTipo;
import com.regenta.recursos.aplicacion.GestionDeTiposDeRecurso;
import com.regenta.recursos.aplicacion.SolicitudDeAtributoDeRecurso;
import com.regenta.recursos.aplicacion.SolicitudDeTipoDeRecurso;
import com.regenta.recursos.aplicacion.TipoDeRecursoDelNegocio;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** Tipos de recurso con atributos configurables. HU-064. */
@RestController
@RequestMapping("/api/recursos/tipos")
@Tag(name = "Tipos de recurso", description = "El catálogo del patrón Reserva: qué se reserva y cómo")
public class TiposDeRecursoControlador {

    private final GestionDeTiposDeRecurso tipos;

    public TiposDeRecursoControlador(GestionDeTiposDeRecurso tipos) {
        this.tipos = tipos;
    }

    @GetMapping
    @Operation(summary = "Los tipos de recurso activos del negocio")
    public List<TipoDeRecursoDelNegocio> listar() {
        return tipos.listar();
    }

    @GetMapping("/{tipoId}")
    @Operation(summary = "Un tipo de recurso con sus atributos")
    @ApiResponse(responseCode = "404", description = "No existe en este negocio")
    public TipoDeRecursoDelNegocio ver(@PathVariable UUID tipoId) {
        return tipos.ver(tipoId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Define un tipo de recurso: unidad de tiempo, granularidad y buffers")
    @ApiResponse(responseCode = "409", description = "Ya hay un tipo con ese nombre")
    public TipoDeRecursoDelNegocio crear(@Valid @RequestBody SolicitudDeTipoDeRecurso solicitud) {
        return tipos.crear(solicitud);
    }

    @PutMapping("/{tipoId}")
    @Operation(summary = "Cambia los datos de un tipo de recurso")
    public TipoDeRecursoDelNegocio actualizar(@PathVariable UUID tipoId,
            @Valid @RequestBody SolicitudDeTipoDeRecurso solicitud) {
        return tipos.actualizar(tipoId, solicitud);
    }

    @DeleteMapping("/{tipoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Desactiva un tipo de recurso")
    public void desactivar(@PathVariable UUID tipoId) {
        tipos.desactivar(tipoId);
    }

    @GetMapping("/{tipoId}/atributos")
    @Operation(summary = "Los atributos configurables del tipo")
    public List<AtributoDelTipo> atributos(@PathVariable UUID tipoId) {
        return tipos.atributos(tipoId);
    }

    @PostMapping("/{tipoId}/atributos")
    @Operation(summary = "Agrega (o actualiza) un atributo del tipo")
    public AtributoDelTipo agregarAtributo(@PathVariable UUID tipoId,
            @Valid @RequestBody SolicitudDeAtributoDeRecurso solicitud) {
        return tipos.agregarAtributo(tipoId, solicitud);
    }

    @DeleteMapping("/atributos/{atributoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Quita un atributo de un tipo")
    public void quitarAtributo(@PathVariable UUID atributoId) {
        tipos.quitarAtributo(atributoId);
    }
}
