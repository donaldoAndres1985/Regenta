package com.regenta.reportes.aplicacion;

/** El archivo exportado, para descargarlo. */
public record ArchivoDeReporte(String nombre, String tipoMime, byte[] contenido) {
}
