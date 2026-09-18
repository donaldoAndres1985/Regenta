package com.regenta.auditoria.aplicacion;

import java.util.List;

/**
 * {@code completaForzada}: true cuando el dispositivo llevaba más del
 * período de retención sin sincronizar y, sin importar el cursor que pidió,
 * se le mandó todo desde el principio (HU-104 criterio 4).
 */
public record ResultadoDeDescarga(List<CambioDeServidor> cambios, boolean completaForzada) {
}
