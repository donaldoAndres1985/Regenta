package com.regenta.compras.web;

import java.util.List;
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

import com.regenta.compras.application.DevolucionAProveedorDelNegocio;
import com.regenta.compras.application.GestionDeDevolucionesAProveedor;
import com.regenta.compras.application.SolicitudDeDevolucionAProveedor;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** HU-178 · Devoluciones a proveedor con nota crédito. */
@RestController
@RequestMapping("/api/compras/devoluciones")
@Tag(name = "Devoluciones a proveedor", description = "Mercancía que vuelve al proveedor, con su nota crédito")
public class DevolucionesAProveedorControlador {

    private final GestionDeDevolucionesAProveedor devoluciones;

    public DevolucionesAProveedorControlador(GestionDeDevolucionesAProveedor devoluciones) {
        this.devoluciones = devoluciones;
    }

    @GetMapping
    @Operation(summary = "Las devoluciones del negocio, opcionalmente las de una recepción")
    public List<DevolucionAProveedorDelNegocio> listar(@RequestParam(required = false) UUID recepcionId) {
        return devoluciones.listar(recepcionId);
    }

    @GetMapping("/{devolucionId}")
    @Operation(summary = "Una devolución con sus líneas y su nota crédito")
    @ApiResponse(responseCode = "404", description = "No existe en este negocio")
    public DevolucionAProveedorDelNegocio ver(@PathVariable UUID devolucionId) {
        return devoluciones.ver(devolucionId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Devuelve al proveedor mercancía de una recepción confirmada",
            description = "Sale de la bodega del lote de origen al costo de entrada (evento "
                    + "devolucion_proveedor_registrada). La nota crédito vale lo facturado de lo devuelto "
                    + "(base con descuentos + IVA; el flete no se devuelve): baja el saldo de la cuenta de la "
                    + "recepción y lo que sobra queda como saldo a favor con el proveedor.")
    @ApiResponse(responseCode = "403", description = "Sin el permiso COMPRAS_DEVOLUCION_REGISTRAR")
    @ApiResponse(responseCode = "404", description = "La recepción o una de sus líneas no existe")
    @ApiResponse(responseCode = "409", description = "La recepción no está confirmada, o no hay en la bodega "
            + "(o en el lote) lo que se quiere devolver")
    @ApiResponse(responseCode = "422", description = "Se devuelve más de lo recibido menos lo ya devuelto, "
            + "o falta el motivo")
    public DevolucionAProveedorDelNegocio registrar(@Valid @RequestBody SolicitudDeDevolucionAProveedor solicitud) {
        return devoluciones.registrar(solicitud);
    }
}
