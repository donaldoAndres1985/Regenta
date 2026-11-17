package com.regenta.inventario.web;

import java.util.List;
import java.util.Map;
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

import com.regenta.inventario.application.AsignacionDeTabla;
import com.regenta.inventario.application.FraccionDelNegocio;
import com.regenta.inventario.application.GestionDeFracciones;
import com.regenta.inventario.application.SolicitudDeTablaDeFracciones;
import com.regenta.inventario.application.TablaDeFraccionesDelNegocio;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** HU-156 · tablas de fracciones compartidas y las fracciones de cada producto. */
@RestController
@RequestMapping("/api/inventario")
@Tag(name = "Fracciones", description = "Tablas de fracciones con precio fijo (½, ¼, ⅛ de galon)")
public class FraccionesControlador {

    private final GestionDeFracciones fracciones;

    public FraccionesControlador(GestionDeFracciones fracciones) {
        this.fracciones = fracciones;
    }

    @GetMapping("/fracciones/tablas")
    @Operation(summary = "Las tablas de fracciones del negocio, con cuantos productos usan cada una")
    public List<TablaDeFraccionesDelNegocio> tablas() {
        return fracciones.listarTablas();
    }

    @PostMapping("/fracciones/tablas")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crea una tabla de fracciones con precios escritos a mano")
    public TablaDeFraccionesDelNegocio crear(@Valid @RequestBody SolicitudDeTablaDeFracciones solicitud) {
        return fracciones.crearTabla(solicitud);
    }

    @PutMapping("/fracciones/tablas/{tablaId}")
    @Operation(summary = "Cambia nombre y fracciones; aplica a todos los productos de la tabla")
    public TablaDeFraccionesDelNegocio editar(@PathVariable UUID tablaId,
            @Valid @RequestBody SolicitudDeTablaDeFracciones solicitud) {
        return fracciones.editarTabla(tablaId, solicitud);
    }

    @PostMapping("/fracciones/tablas/{tablaId}/asignacion")
    @Operation(summary = "Asigna productos a la tabla; todo o nada")
    @ApiResponse(responseCode = "409", description = "Alguno ya tiene otra tabla: reenviar con confirmarCambio")
    public Map<String, Integer> asignar(@PathVariable UUID tablaId,
            @Valid @RequestBody AsignacionDeTabla asignacion) {
        return Map.of("asignados", fracciones.asignar(tablaId, asignacion));
    }

    @PutMapping("/productos/{productoId}/fracciones/precio-propio")
    @Operation(summary = "Fija el precio propio de una fraccion para este producto (HU-157)")
    public void fijarPrecioPropio(@PathVariable UUID productoId, @RequestBody PrecioPropio cuerpo) {
        fracciones.fijarPrecioPropio(productoId, cuerpo.medida(), cuerpo.precio());
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/productos/{productoId}/fracciones/precio-propio")
    @Operation(summary = "Quita el precio propio: vuelve al de la tabla (HU-157)")
    public void quitarPrecioPropio(@PathVariable UUID productoId,
            @org.springframework.web.bind.annotation.RequestParam String medida) {
        fracciones.quitarPrecioPropio(productoId, medida);
    }

    public record PrecioPropio(String medida, java.math.BigDecimal precio) {
    }

    @GetMapping("/productos/{productoId}/fracciones")
    @Operation(summary = "Las fracciones con que se vende un producto, con su precio y su origen")
    public List<FraccionDelNegocio> deProducto(@PathVariable UUID productoId) {
        return fracciones.fraccionesDe(productoId);
    }
}
