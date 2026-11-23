package com.regenta.shared.almacen;

import java.util.UUID;

/**
 * HU-121 · Guarda documentos y archivos fuera de la base: en la base solo queda la URL que devuelve
 * {@link #guardar} (C1).
 *
 * <p>Los objetos no son públicos. Cada uno vive bajo la carpeta de su negocio, y {@link #leer} y
 * {@link #borrar} exigen ese negocio: con otro, el objeto «no existe» (C2). Quien descarga pasa
 * siempre por un endpoint del backend con la sesión del negocio dueño; nunca se entrega una URL
 * directa al almacén.
 */
public interface AlmacenDeObjetos {

    /**
     * Guarda el objeto y devuelve su URL estable ({@code s3://bucket/negocio/carpeta/…}).
     *
     * @throws AlmacenNoDisponibleException si el almacén no responde: quien llama decide si reintenta
     */
    String guardar(UUID negocioId, String carpeta, String nombre, byte[] contenido, String tipoMime);

    /**
     * @throws com.regenta.shared.web.NoEncontradoException si la URL no es de ese negocio o no existe
     * @throws AlmacenNoDisponibleException si el almacén no responde
     */
    byte[] leer(UUID negocioId, String url);

    /** Borra el objeto; si ya no estaba, no pasa nada. */
    void borrar(UUID negocioId, String url);
}
