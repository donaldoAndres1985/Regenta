package com.regenta.facturacion.aplicacion;

import java.util.List;

import com.regenta.facturacion.domain.LineaFacturable;

import jakarta.validation.constraints.Size;

/**
 * Alta manual de una nota crédito. Si {@code lineas} viene vacío, la NC copia
 * las líneas de la factura de origen (NC total).
 */
public record SolicitudDeNotaCredito(
        @Size(max = 10) String codigoNota,
        @Size(max = 200) String motivo,
        List<LineaFacturable> lineas) {

    public List<LineaFacturable> lineas() {
        return lineas == null ? List.of() : lineas;
    }
}
