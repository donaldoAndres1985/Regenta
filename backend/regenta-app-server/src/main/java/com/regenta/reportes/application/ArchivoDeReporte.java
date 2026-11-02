package com.regenta.reportes.application;

/** El archivo exportado, para descargarlo. */
public record ArchivoDeReporte(String nombre, String tipoMime, byte[] contenido) {
}
