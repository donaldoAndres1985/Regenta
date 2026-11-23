package com.regenta.shared.almacen;

import java.text.Normalizer;
import java.util.Locale;
import java.util.UUID;

import com.regenta.shared.web.NoEncontradoException;

/**
 * HU-121 · Dónde vive un objeto: {@code <negocio>/<carpeta>/<uuid>/<nombre>}. El negocio va primero
 * para que el dueño de cada objeto se lea de su propia clave (C2), y el uuid para que dos archivos con
 * el mismo nombre no se pisen.
 */
final class ClaveDeObjeto {

    private ClaveDeObjeto() {
    }

    static String nueva(UUID negocioId, String carpeta, String nombre) {
        if (negocioId == null) {
            throw new IllegalArgumentException("Todo objeto es de un negocio");
        }
        return negocioId + "/" + segmento(carpeta, "archivos") + "/" + UUID.randomUUID() + "/"
                + segmento(nombre, "archivo");
    }

    /** La clave dentro de la URL {@code <esquema>://<bucket>/<clave>}, si es de ese negocio. */
    static String deLaUrl(String url, String esquema, String bucket, UUID negocioId) {
        String prefijo = esquema + "://" + bucket + "/";
        if (url == null || !url.startsWith(prefijo)) {
            throw new NoEncontradoException("Ese documento no existe");
        }
        String clave = url.substring(prefijo.length());
        if (negocioId == null || !clave.startsWith(negocioId + "/") || clave.contains("..")) {
            throw new NoEncontradoException("Ese documento no existe");
        }
        return clave;
    }

    /** Sin acentos ni nada que parezca una ruta: letras, números, punto, guion y guion bajo. */
    static String segmento(String texto, String siVacio) {
        String limpio = Normalizer.normalize(texto == null ? "" : texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^A-Za-z0-9._-]+", "-")
                .replaceAll("^[.-]+", "")
                .toLowerCase(Locale.ROOT);
        if (limpio.isBlank()) {
            return siVacio;
        }
        return limpio.length() > 120 ? limpio.substring(limpio.length() - 120) : limpio;
    }
}
