package com.regenta.mesas.application;

import com.regenta.mesas.api.*;

/** Mover o redimensionar una mesa en el plano (HU-081 criterio 3). */
public record SolicitudDePosicion(Integer posX, Integer posY, Integer ancho, Integer alto) {
}
