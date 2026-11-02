package com.regenta.inventario.application;

import com.regenta.inventario.api.*;

import java.util.List;

import jakarta.validation.Valid;

/**
 * La recepciÃ³n de un traslado. Una lista vacÃ­a recibe todo lo enviado en cada
 * lÃ­nea; con lÃ­neas, cada una dice cuÃ¡nto llegÃ³ de verdad (recepciÃ³n parcial).
 */
public record SolicitudDeRecepcion(
        @Valid List<LineaRecibida> lineas) {
}
