package com.regenta.facturacion.infra;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import com.regenta.facturacion.aplicacion.AlmacenDeDocumentos;

/**
 * Stub del storage de documentos. Guarda en memoria y devuelve una URL
 * {@code stub://…}. La integración real (S3 / R2) reemplaza este bean. El XML
 * NUNCA va a la base.
 */
@Component
public class AlmacenDeDocumentosStub implements AlmacenDeDocumentos {

    private final Map<String, byte[]> objetos = new ConcurrentHashMap<>();

    @Override
    public String guardar(UUID negocioId, String nombre, byte[] contenido, String contentType) {
        String url = "stub://facturacion/" + negocioId + "/" + UUID.randomUUID() + "/" + nombre;
        objetos.put(url, contenido);
        return url;
    }

    @Override
    public byte[] leer(String url) {
        byte[] c = objetos.get(url);
        if (c == null) {
            throw new IllegalStateException("No hay documento en " + url);
        }
        return c;
    }
}
