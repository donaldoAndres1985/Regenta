package com.regenta.comun.barridos;

import java.time.Duration;
import java.util.Set;
import java.util.function.IntSupplier;

/**
 * Un trabajo de fondo que hay que hacer en todos los negocios (HU-124): el
 * timeout de la saga, el reintento de entregas, los seguimientos del CRM...
 *
 * @param nombre                el que se usa en la configuración:
 *                              {@code regenta.barridos.<nombre>.intervalo}
 * @param sqlNegociosPendientes devuelve los {@code negocio_id} con trabajo
 *                              pendiente. Corre con el rol privilegiado, que ve
 *                              todos los negocios: debe ser barata y no mirar
 *                              más que lo necesario para decidir.
 * @param modulos               los módulos con que corre la tarea
 * @param permisos              los permisos con que corre la tarea: los mismos
 *                              que pide el endpoint manual del barrido
 * @param intervaloPorDefecto   cada cuánto, si la configuración no dice otra cosa
 * @param tarea                 lo que hace el endpoint manual, dentro del negocio
 *                              ya fijado; devuelve cuántas filas tomó
 */
public record Barrido(String nombre, String sqlNegociosPendientes, Set<String> modulos,
        Set<String> permisos, Duration intervaloPorDefecto, IntSupplier tarea) {
}
