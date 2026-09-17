package com.regenta.comandas.api;

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

import com.regenta.comandas.aplicacion.CuentaDetallada;
import com.regenta.comandas.aplicacion.GestionDeCuentas;
import com.regenta.comandas.aplicacion.SolicitudDeCuenta;
import com.regenta.comandas.aplicacion.SolicitudDeDivisionIgual;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** Dividir la cuenta entre comensales. HU-089. */
@RestController
@RequestMapping("/api/comandas/{comandaId}/cuentas")
@Tag(name = "Cuentas", description = "Partir la cuenta por ítem, en partes iguales o por monto")
public class CuentasControlador {

    private final GestionDeCuentas cuentas;

    public CuentasControlador(GestionDeCuentas cuentas) {
        this.cuentas = cuentas;
    }

    @GetMapping
    @Operation(summary = "Las cuentas en que se dividió la comanda")
    public List<CuentaDetallada> ver(@PathVariable UUID comandaId) {
        return cuentas.ver(comandaId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crea una cuenta vacía sobre la que se marcan líneas")
    public CuentaDetallada crear(@PathVariable UUID comandaId, @Valid @RequestBody SolicitudDeCuenta solicitud) {
        return cuentas.crearCuenta(comandaId, solicitud);
    }

    @PostMapping("/division-igual")
    @Operation(summary = "Divide el total de la comanda en N cuentas iguales")
    public List<CuentaDetallada> dividirEnPartesIguales(@PathVariable UUID comandaId,
            @Valid @RequestBody SolicitudDeDivisionIgual solicitud) {
        return cuentas.dividirEnPartesIguales(comandaId, solicitud.numeroPartes());
    }

    @PutMapping("/{cuentaId}/lineas/{lineaId}")
    @Operation(summary = "Marca una línea en esta cuenta; si ya estaba en otras, el reparto se recalcula")
    @ApiResponse(responseCode = "409", description = "La cuenta ya está pagada: no se le mueven líneas")
    public List<CuentaDetallada> marcarLinea(@PathVariable UUID comandaId, @PathVariable UUID cuentaId,
            @PathVariable UUID lineaId) {
        return cuentas.marcarLinea(comandaId, cuentaId, lineaId);
    }

    @DeleteMapping("/{cuentaId}/lineas/{lineaId}")
    @Operation(summary = "Desmarca una línea de esta cuenta")
    @ApiResponse(responseCode = "409", description = "La cuenta ya está pagada: no se le mueven líneas")
    public List<CuentaDetallada> desmarcarLinea(@PathVariable UUID comandaId, @PathVariable UUID cuentaId,
            @PathVariable UUID lineaId) {
        return cuentas.desmarcarLinea(comandaId, cuentaId, lineaId);
    }

    @PostMapping("/{cuentaId}/pago")
    @Operation(summary = "Cobra la cuenta entera; si era la última abierta, la comanda se cierra sola")
    public CuentaDetallada marcarPagada(@PathVariable UUID comandaId, @PathVariable UUID cuentaId) {
        return cuentas.marcarPagada(comandaId, cuentaId);
    }
}
