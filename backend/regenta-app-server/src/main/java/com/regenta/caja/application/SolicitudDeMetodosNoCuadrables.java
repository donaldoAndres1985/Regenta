package com.regenta.caja.application;

import java.util.List;

import jakarta.validation.constraints.NotNull;

/**
 * HU-139 · los métodos de pago que el negocio no cuadra al cerrar la caja. Una lista vacía
 * significa que se cuadran todos.
 */
public record SolicitudDeMetodosNoCuadrables(@NotNull List<String> metodos) {
}
