package com.regenta.reportes.aplicacion;

import java.time.LocalDate;
import java.util.List;

/**
 * Un reporte que se puede exportar o programar (HU-100).
 *
 * <p>No hay un motor de consultas genérico: {@code definiciones_reporte.consulta}
 * existe en el esquema desde V1 pero está vacío a propósito. Un lenguaje de
 * consulta declarativo sobre el esquema en estrella es un proyecto entero, y
 * lo que la historia pide es exportar los reportes que ya existen. Cada uno
 * se registra como un bean y el catálogo los junta.
 */
public interface ReporteExportable {

    /** Lo que se guarda en {@code ejecuciones_reporte.codigo}. */
    String codigo();

    String nombre();

    /** {@code null} = sirve para cualquier patrón operativo. */
    default String patron() {
        return null;
    }

    List<String> columnas();

    List<List<Object>> filas(LocalDate desde, LocalDate hasta);
}
