package com.regenta.mesas.application;

import com.regenta.mesas.api.*;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** Abrir una sesiÃ³n de mesa (HU-082 criterio 1). */
public record SolicitudDeApertura(@NotNull @Positive Integer numComensales) {
}
