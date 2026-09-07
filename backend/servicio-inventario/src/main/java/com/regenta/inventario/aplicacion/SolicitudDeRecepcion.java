package com.regenta.inventario.aplicacion;

import java.util.List;

import jakarta.validation.Valid;

/**
 * La recepción de un traslado. Una lista vacía recibe todo lo enviado en cada
 * línea; con líneas, cada una dice cuánto llegó de verdad (recepción parcial).
 */
public record SolicitudDeRecepcion(
        @Valid List<LineaRecibida> lineas) {
}
