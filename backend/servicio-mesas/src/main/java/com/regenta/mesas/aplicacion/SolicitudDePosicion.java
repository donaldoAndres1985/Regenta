package com.regenta.mesas.aplicacion;

/** Mover o redimensionar una mesa en el plano (HU-081 criterio 3). */
public record SolicitudDePosicion(Integer posX, Integer posY, Integer ancho, Integer alto) {
}
