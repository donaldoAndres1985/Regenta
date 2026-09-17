package com.regenta.comandas.aplicacion;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Divide el total de la comanda en N cuentas iguales (HU-089 criterio 3). */
public record SolicitudDeDivisionIgual(@NotNull @Min(2) Integer numeroPartes) {
}
