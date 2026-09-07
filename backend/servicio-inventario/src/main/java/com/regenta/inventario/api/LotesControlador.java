package com.regenta.inventario.api;

import java.math.BigDecimal;
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

import com.regenta.inventario.aplicacion.ExistenciaDeLote;
import com.regenta.inventario.aplicacion.GestionDeLotes;
import com.regenta.inventario.aplicacion.RegistroDeLote;
import com.regenta.inventario.aplicacion.RegistroDeMovimiento;
import com.regenta.inventario.aplicacion.SolicitudDeEntradaDeLote;
import com.regenta.inventario.aplicacion.SolicitudDeSalidaDeLote;
import com.regenta.inventario.aplicacion.SugerenciaFefo;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** Lotes y fechas de vencimiento. HU-031. */
@RestController
@RequestMapping("/api/inventario/lotes")
@Tag(name = "Lotes", description = "Control de lotes, vencimientos y FEFO")
public class LotesControlador {

    private final GestionDeLotes lotes;

    public LotesControlador(GestionDeLotes lotes) {
        this.lotes = lotes;
    }

    @PostMapping("/entradas")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registra la entrada de un lote; exige el codigo de lote")
    @ApiResponse(responseCode = "422",
            description = "El producto no maneja lotes o falta el codigo de lote")
    public RegistroDeLote registrarEntrada(@Valid @RequestBody SolicitudDeEntradaDeLote solicitud) {
        return lotes.registrarEntrada(solicitud);
    }

    @PostMapping("/salidas")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Da salida a un lote; el lote vencido exige autorizacion explicita")
    @ApiResponse(responseCode = "422",
            description = "Lote vencido sin autorizacion, o sin saldo suficiente en la bodega")
    public RegistroDeMovimiento registrarSalida(@Valid @RequestBody SolicitudDeSalidaDeLote solicitud) {
        return lotes.registrarSalida(solicitud);
    }

    @GetMapping("/{loteId}/existencia")
    @Operation(summary = "La existencia de un lote, desglosada por bodega")
    public ExistenciaDeLote existencia(@PathVariable UUID loteId) {
        return lotes.existenciaDelLote(loteId);
    }

    @GetMapping("/fefo")
    @Operation(summary = "Sugerencia FEFO: de que lotes tomar para una salida")
    public SugerenciaFefo fefo(@RequestParam UUID productoId, @RequestParam UUID bodegaId,
            @RequestParam BigDecimal cantidad) {
        return lotes.sugerirFefo(productoId, bodegaId, cantidad);
    }
}
