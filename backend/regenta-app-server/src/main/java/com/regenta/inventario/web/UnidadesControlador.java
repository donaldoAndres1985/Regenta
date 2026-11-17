package com.regenta.inventario.web;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.regenta.inventario.application.GestionDeUnidades;
import com.regenta.inventario.application.SolicitudDeUnidad;
import com.regenta.inventario.application.UnidadDelNegocio;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** Unidades de medida del negocio (HU-150 para elegir, HU-155 para administrarlas). */
@RestController
@RequestMapping("/api/inventario/unidades")
@Tag(name = "Unidades", description = "Unidades de medida del negocio")
public class UnidadesControlador {

    private final GestionDeUnidades unidades;

    public UnidadesControlador(GestionDeUnidades unidades) {
        this.unidades = unidades;
    }

    @GetMapping
    @Operation(summary = "Las unidades de medida del negocio")
    public List<UnidadDelNegocio> listar() {
        return unidades.listar();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crea una unidad (HU-155); el codigo no se repite en el negocio")
    public UnidadDelNegocio crear(@Valid @RequestBody SolicitudDeUnidad solicitud) {
        return unidades.crear(solicitud);
    }

    @PutMapping("/{unidadId}")
    @Operation(summary = "Cambia el nombre y si admite decimales (HU-155)")
    public UnidadDelNegocio editar(@PathVariable UUID unidadId,
            @Valid @RequestBody SolicitudDeUnidad solicitud) {
        return unidades.editar(unidadId, solicitud);
    }

    @PostMapping("/{unidadId}/desactivacion")
    @Operation(summary = "Desactiva una unidad que ningun producto usa (HU-155)")
    public UnidadDelNegocio desactivar(@PathVariable UUID unidadId) {
        return unidades.desactivar(unidadId);
    }
}
