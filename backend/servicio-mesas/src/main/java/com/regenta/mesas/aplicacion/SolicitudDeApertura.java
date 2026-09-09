package com.regenta.mesas.aplicacion;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** Abrir una sesión de mesa (HU-082 criterio 1). */
public record SolicitudDeApertura(@NotNull @Positive Integer numComensales) {
}
