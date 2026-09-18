package com.regenta.ventas.aplicacion;

/**
 * Confirmar una venta a plazo (HU-040).
 *
 * @param autorizado el vendedor ya vio la advertencia de cartera vencida y
 *        alguien con permiso decide seguir igual (criterio 4). En el caso
 *        normal va en false: la advertencia tiene que aparecer una vez, no
 *        pasarse por alto sin querer.
 */
public record SolicitudDeVentaACredito(boolean autorizado) {
}
