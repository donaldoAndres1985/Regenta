package com.regenta.inventario.api;

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

import com.regenta.inventario.aplicacion.AtributoDeCategoria;
import com.regenta.inventario.aplicacion.CategoriaDelNegocio;
import com.regenta.inventario.aplicacion.GestionDeAtributos;
import com.regenta.inventario.aplicacion.GestionDeCategorias;
import com.regenta.inventario.aplicacion.SolicitudDeAtributo;
import com.regenta.inventario.aplicacion.SolicitudDeCategoria;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** Categorias y sus atributos. HU-026 y HU-027. */
@RestController
@RequestMapping("/api/inventario/categorias")
@Tag(name = "Categorias", description = "El catalogo configurable: categorias y los campos que exigen")
public class CategoriasControlador {

    private final GestionDeCategorias categorias;
    private final GestionDeAtributos atributos;

    public CategoriasControlador(GestionDeCategorias categorias, GestionDeAtributos atributos) {
        this.categorias = categorias;
        this.atributos = atributos;
    }

    @GetMapping
    @Operation(summary = "Las categorias del negocio, en orden de jerarquia")
    public List<CategoriaDelNegocio> listar() {
        return categorias.listar();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crea una categoria; con padre, hereda sus atributos heredables")
    @ApiResponse(responseCode = "409", description = "Ya hay una categoria con ese nombre y padre")
    public CategoriaDelNegocio crear(@Valid @RequestBody SolicitudDeCategoria solicitud) {
        return categorias.crear(solicitud);
    }

    @PutMapping("/{categoriaId}")
    @Operation(summary = "Renombra o cambia los datos de una categoria")
    public CategoriaDelNegocio actualizar(@PathVariable UUID categoriaId,
            @Valid @RequestBody SolicitudDeCategoria solicitud) {
        return categorias.actualizar(categoriaId, solicitud);
    }

    @DeleteMapping("/{categoriaId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Elimina una categoria vacia")
    @ApiResponse(responseCode = "409", description = "Tiene productos o subcategorias")
    public void eliminar(@PathVariable UUID categoriaId) {
        categorias.eliminar(categoriaId);
    }

    @GetMapping("/{categoriaId}/atributos")
    @Operation(summary = "Los campos que exige esta categoria")
    public List<AtributoDeCategoria> atributos(@PathVariable UUID categoriaId) {
        return atributos.listar(categoriaId);
    }

    @PostMapping("/{categoriaId}/atributos")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Define un campo extra para la categoria")
    @ApiResponse(responseCode = "409", description = "Ya existe un atributo con ese nombre de campo")
    @ApiResponse(responseCode = "422", description = "Forma invalida: LISTA sin opciones, rango sin tipo numerico")
    public AtributoDeCategoria crearAtributo(@PathVariable UUID categoriaId,
            @Valid @RequestBody SolicitudDeAtributo solicitud) {
        return atributos.crear(categoriaId, solicitud);
    }

    @PutMapping("/atributos/{atributoId}")
    @Operation(summary = "Cambia un atributo")
    public AtributoDeCategoria actualizarAtributo(@PathVariable UUID atributoId,
            @Valid @RequestBody SolicitudDeAtributo solicitud) {
        return atributos.actualizar(atributoId, solicitud);
    }

    @DeleteMapping("/atributos/{atributoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Quita un atributo de la categoria")
    public void eliminarAtributo(@PathVariable UUID atributoId) {
        atributos.eliminar(atributoId);
    }
}
