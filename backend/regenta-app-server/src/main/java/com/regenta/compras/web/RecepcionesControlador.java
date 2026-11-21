package com.regenta.compras.web;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.regenta.compras.application.GestionDeRecepciones;
import com.regenta.compras.application.RecepcionDelNegocio;
import com.regenta.compras.application.SolicitudDeAnulacionDeRecepcion;
import com.regenta.compras.application.SolicitudDeCompraDirecta;
import com.regenta.compras.application.SolicitudDeConfirmacionDeRecepcion;
import com.regenta.compras.application.SolicitudDeRecepcion;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** Recepción de mercancía, contra una orden o como compra directa. HU-048 y HU-167. */
@RestController
@RequestMapping("/api/compras/recepciones")
@Tag(name = "Recepciones", description = "Lo que llega del proveedor, con su factura y sus lotes")
public class RecepcionesControlador {

    private final GestionDeRecepciones recepciones;

    public RecepcionesControlador(GestionDeRecepciones recepciones) {
        this.recepciones = recepciones;
    }

    @GetMapping
    @Operation(summary = "Las recepciones del negocio, opcionalmente las de una orden")
    public List<RecepcionDelNegocio> listar(@RequestParam(required = false) UUID ordenId) {
        return recepciones.listar(ordenId);
    }

    @GetMapping("/{recepcionId}")
    @Operation(summary = "Una recepción con su factura, sus líneas y los lotes capturados")
    @ApiResponse(responseCode = "404", description = "No existe en este negocio")
    public RecepcionDelNegocio ver(@PathVariable UUID recepcionId) {
        return recepciones.ver(recepcionId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Arma en borrador lo que llegó de una orden",
            description = "HU-175 · Con la factura como en la compra directa: número, fecha, flete, "
                    + "descuento global y contado o crédito.")
    @ApiResponse(responseCode = "409", description = "La orden está en borrador, anulada o recibida, "
            + "o la factura ya está registrada para ese proveedor")
    @ApiResponse(responseCode = "422", description = "Falta el lote donde el producto lo exige, una línea "
            + "pasa lo pedido más la tolerancia, o de contado falta la factura o el método de pago")
    public RecepcionDelNegocio crear(@Valid @RequestBody SolicitudDeRecepcion solicitud) {
        return recepciones.crear(solicitud);
    }

    @PostMapping("/directas")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Compra directa: arma en borrador lo que llegó con la factura, sin orden",
            description = "La tarifa de IVA y la exigencia de lote salen del catálogo; sin costo, "
                    + "se usa el último del catálogo del proveedor.")
    @ApiResponse(responseCode = "404", description = "El proveedor, la bodega o un producto no existen")
    @ApiResponse(responseCode = "409", description = "La factura ya está registrada para ese proveedor")
    @ApiResponse(responseCode = "422", description = "Proveedor desactivado, falta el costo o el lote")
    public RecepcionDelNegocio crearDirecta(@Valid @RequestBody SolicitudDeCompraDirecta solicitud) {
        return recepciones.crearDirecta(solicitud);
    }

    @PutMapping("/{recepcionId}")
    @Operation(summary = "Corrige una compra directa en borrador (factura y líneas)")
    @ApiResponse(responseCode = "409", description = "La recepción ya está confirmada o es de una orden")
    public RecepcionDelNegocio actualizarDirecta(@PathVariable UUID recepcionId,
            @Valid @RequestBody SolicitudDeCompraDirecta solicitud) {
        return recepciones.actualizarDirecta(recepcionId, solicitud);
    }

    @PostMapping("/{recepcionId}/confirmacion")
    @Operation(summary = "Confirma la recepción: sube inventario, actualiza el catálogo del "
            + "proveedor, avanza la orden y abre la cuenta por pagar")
    @ApiResponse(responseCode = "409", description = "Ya no está en borrador, o la factura ya está registrada")
    @ApiResponse(responseCode = "422", description = "La cantidad recibida supera lo pedido + 5 %")
    public RecepcionDelNegocio confirmar(@PathVariable UUID recepcionId,
            @RequestBody(required = false) SolicitudDeConfirmacionDeRecepcion solicitud) {
        return recepciones.confirmar(recepcionId, solicitud);
    }

    @PostMapping("/{recepcionId}/anulacion")
    @Operation(summary = "Anula una recepción confirmada mal registrada (HU-177)",
            description = "Con motivo. La cuenta por pagar queda ANULADA, la orden recupera el faltante e "
                    + "Inventario saca lo que entró, del mismo lote (evento recepcion_anulada). La "
                    + "recepción sigue visible con quién, cuándo y por qué.")
    @ApiResponse(responseCode = "403", description = "Sin el permiso COMPRAS_COMPRA_ANULAR")
    @ApiResponse(responseCode = "409", description = "No está confirmada, su cuenta tiene abonos («Primero "
            + "anula los pagos de esta factura.») o ya salió parte de la mercancía")
    @ApiResponse(responseCode = "422", description = "Falta el motivo")
    public RecepcionDelNegocio anular(@PathVariable UUID recepcionId,
            @Valid @RequestBody SolicitudDeAnulacionDeRecepcion solicitud) {
        return recepciones.anular(recepcionId, solicitud);
    }
}
