package com.regenta.facturacion.application;

import java.util.UUID;

/**
 * Guarda el XML (y luego el PDF) FUERA de la base: un objeto en el almacén (S3, R2…). En la base
 * solo queda la URL (HU-055 criterio 1, HU-121 C1).
 *
 * <p>HU-121 C3: si el almacén no responde, la emisión no se pierde. {@link #guardar} devuelve una URL
 * provisional, el documento se puede {@link #leer} igual (la transmisión sigue) y un barrido lo sube
 * cuando el almacén vuelve.
 */
public interface AlmacenDeDocumentos {

    String guardar(UUID negocioId, String nombre, byte[] contenido, String contentType);

    /** Solo el negocio dueño lee su documento (HU-121 C2). */
    byte[] leer(UUID negocioId, String url);
}
