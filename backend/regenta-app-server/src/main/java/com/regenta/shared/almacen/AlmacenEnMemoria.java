package com.regenta.shared.almacen;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.regenta.shared.web.NoEncontradoException;

/**
 * HU-121 · El almacén sin S3 configurado: en memoria, para desarrollo y pruebas. Cumple las mismas
 * reglas (clave por negocio, C2) para que nada dependa de cuál está puesto. Se pierde al reiniciar:
 * en producción va {@link AlmacenS3}.
 */
public class AlmacenEnMemoria implements AlmacenDeObjetos {

    static final String ESQUEMA = "memoria";
    static final String BUCKET = "regenta";

    private final Map<String, byte[]> objetos = new ConcurrentHashMap<>();
    private volatile boolean caido;

    @Override
    public String guardar(UUID negocioId, String carpeta, String nombre, byte[] contenido, String tipoMime) {
        exigirArriba();
        String clave = ClaveDeObjeto.nueva(negocioId, carpeta, nombre);
        objetos.put(clave, contenido.clone());
        return ESQUEMA + "://" + BUCKET + "/" + clave;
    }

    @Override
    public byte[] leer(UUID negocioId, String url) {
        exigirArriba();
        byte[] c = objetos.get(ClaveDeObjeto.deLaUrl(url, ESQUEMA, BUCKET, negocioId));
        if (c == null) {
            throw new NoEncontradoException("Ese documento no existe");
        }
        return c.clone();
    }

    @Override
    public void borrar(UUID negocioId, String url) {
        exigirArriba();
        objetos.remove(ClaveDeObjeto.deLaUrl(url, ESQUEMA, BUCKET, negocioId));
    }

    /** Para las pruebas: cuántos objetos hay. */
    public int cuantos() {
        return objetos.size();
    }

    /** Para las pruebas de C3: el almacén deja de responder (o vuelve). */
    public void simularCaida(boolean caido) {
        this.caido = caido;
    }

    private void exigirArriba() {
        if (caido) {
            throw new AlmacenNoDisponibleException("El almacén de documentos no responde", null);
        }
    }
}
