package com.regenta.inventario.api;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.regenta.inventario.aplicacion.BusquedaDeProductos;
import com.regenta.inventario.aplicacion.CodigoResuelto;
import com.regenta.inventario.aplicacion.FiltroDeBusqueda;
import com.regenta.inventario.aplicacion.ResultadoDeBusqueda;
import com.regenta.inventario.aplicacion.SolicitudDeCodigo;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** Búsqueda de productos y resolución de código de barras. HU-035. */
@RestController
@RequestMapping("/api/inventario/productos")
@Tag(name = "Búsqueda de productos", description = "Encontrar un producto por texto o por código")
public class BusquedaControlador {

    private final BusquedaDeProductos busqueda;

    public BusquedaControlador(BusquedaDeProductos busqueda) {
        this.busqueda = busqueda;
    }

    @GetMapping("/buscar")
    @Operation(summary = "Busca por nombre, SKU o código; sin término lista todos")
    @ApiResponse(responseCode = "422", description = "El término tiene menos de tres caracteres")
    public ResultadoDeBusqueda buscar(@RequestParam(required = false) String q,
            @RequestParam(required = false) UUID categoriaId,
            @RequestParam(required = false, defaultValue = "false") boolean soloBajoMinimo,
            @RequestParam(required = false) Integer limite) {
        return busqueda.buscar(new FiltroDeBusqueda(q, categoriaId, soloBajoMinimo, limite));
    }

    @GetMapping("/codigo/{codigo}")
    @Operation(summary = "Resuelve un código de barras (propio o alterno) al producto y su factor")
    @ApiResponse(responseCode = "404", description = "Ningún producto tiene ese código")
    public CodigoResuelto resolverCodigo(@PathVariable String codigo) {
        return busqueda.resolverCodigo(codigo);
    }

    @PostMapping("/{productoId}/codigos")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Agrega un código de barras alterno a un producto")
    @ApiResponse(responseCode = "409", description = "Ya hay un producto con ese código")
    public void agregarCodigo(@PathVariable UUID productoId,
            @Valid @RequestBody SolicitudDeCodigo solicitud) {
        busqueda.agregarCodigo(productoId, solicitud);
    }
}
