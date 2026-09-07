package com.regenta.facturacion.aplicacion;

import java.util.UUID;

/**
 * Guarda el XML (y luego el PDF) FUERA de la base: un objeto en storage
 * (S3/R2/…). En la base solo queda la URL (HU-055 criterio 1).
 */
public interface AlmacenDeDocumentos {

    String guardar(UUID negocioId, String nombre, byte[] contenido, String contentType);

    byte[] leer(String url);
}
