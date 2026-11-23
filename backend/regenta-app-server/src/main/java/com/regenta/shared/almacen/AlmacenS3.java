package com.regenta.shared.almacen;

import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.regenta.shared.web.NoEncontradoException;

import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;

/**
 * HU-121 · El almacén de verdad: cualquier servicio con la API de S3 (MinIO en local, Cloudflare R2 o
 * AWS S3 en producción). El bucket es privado: nada se lee sin pasar por el backend (C2). Si el
 * servicio no responde se lanza {@link AlmacenNoDisponibleException} para que quien llama reintente
 * (C3).
 */
public class AlmacenS3 implements AlmacenDeObjetos {

    private static final Logger LOG = LoggerFactory.getLogger(AlmacenS3.class);
    static final String ESQUEMA = "s3";

    private final S3Client s3;
    private final String bucket;

    public AlmacenS3(S3Client s3, String bucket) {
        this.s3 = s3;
        this.bucket = bucket;
    }

    /** Crea el bucket si no existe (para el S3 local; en producción lo crea quien administra). */
    public void asegurarBucket() {
        try {
            s3.headBucket(b -> b.bucket(bucket));
        } catch (S3Exception noEsta) {
            if (noEsta.statusCode() != 404) {
                throw noEsta;
            }
            LOG.info("Creando el bucket {} del almacén de documentos", bucket);
            s3.createBucket(b -> b.bucket(bucket));
        }
    }

    @Override
    public String guardar(UUID negocioId, String carpeta, String nombre, byte[] contenido, String tipoMime) {
        String clave = ClaveDeObjeto.nueva(negocioId, carpeta, nombre);
        try {
            s3.putObject(b -> b.bucket(bucket).key(clave)
                    .contentType(tipoMime == null ? "application/octet-stream" : tipoMime)
                    .metadata(Map.of("negocio", negocioId.toString())),
                    RequestBody.fromBytes(contenido));
        } catch (SdkException fallo) {
            throw new AlmacenNoDisponibleException("No se pudo guardar el documento: " + fallo.getMessage(), fallo);
        }
        return ESQUEMA + "://" + bucket + "/" + clave;
    }

    @Override
    public byte[] leer(UUID negocioId, String url) {
        String clave = ClaveDeObjeto.deLaUrl(url, ESQUEMA, bucket, negocioId);
        try {
            return s3.getObjectAsBytes(b -> b.bucket(bucket).key(clave)).asByteArray();
        } catch (NoSuchKeyException noEsta) {
            throw new NoEncontradoException("Ese documento no existe");
        } catch (SdkException fallo) {
            throw new AlmacenNoDisponibleException("No se pudo leer el documento: " + fallo.getMessage(), fallo);
        }
    }

    @Override
    public void borrar(UUID negocioId, String url) {
        String clave = ClaveDeObjeto.deLaUrl(url, ESQUEMA, bucket, negocioId);
        try {
            s3.deleteObject(b -> b.bucket(bucket).key(clave));
        } catch (SdkException fallo) {
            throw new AlmacenNoDisponibleException("No se pudo borrar el documento: " + fallo.getMessage(), fallo);
        }
    }
}
