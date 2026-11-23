package com.regenta.shared.almacen;

import java.net.URI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * HU-121 · Qué almacén se usa: {@code regenta.almacen.tipo=s3} con su endpoint, bucket y credenciales
 * (todo del entorno, nada en el repo), o en memoria si no se configura.
 */
@Configuration
public class ConfiguracionDelAlmacen {

    @Bean
    @ConditionalOnProperty(name = "regenta.almacen.tipo", havingValue = "s3")
    public AlmacenDeObjetos almacenS3(
            @Value("${regenta.almacen.s3.endpoint:}") String endpoint,
            @Value("${regenta.almacen.s3.region:auto}") String region,
            @Value("${regenta.almacen.s3.bucket:regenta-documentos}") String bucket,
            @Value("${regenta.almacen.s3.access-key:}") String accessKey,
            @Value("${regenta.almacen.s3.secret-key:}") String secretKey,
            @Value("${regenta.almacen.s3.crear-bucket:false}") boolean crearBucket) {
        if (accessKey.isBlank() || secretKey.isBlank()) {
            throw new IllegalStateException(
                    "El almacén S3 necesita ALMACEN_S3_ACCESS_KEY y ALMACEN_S3_SECRET_KEY en el entorno");
        }
        AlmacenS3 almacen = new AlmacenS3(cliente(endpoint, region, accessKey, secretKey), bucket);
        if (crearBucket) {
            almacen.asegurarBucket();
        }
        return almacen;
    }

    @Bean
    @ConditionalOnMissingBean(AlmacenDeObjetos.class)
    public AlmacenDeObjetos almacenEnMemoria() {
        return new AlmacenEnMemoria();
    }

    /** Con endpoint propio (MinIO, R2) va por ruta: {@code endpoint/bucket/clave}. */
    public static S3Client cliente(String endpoint, String region, String accessKey, String secretKey) {
        var constructor = S3Client.builder()
                .httpClient(UrlConnectionHttpClient.builder().build())
                .region(Region.of(region == null || region.isBlank() ? "auto" : region))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)));
        if (endpoint != null && !endpoint.isBlank()) {
            constructor.endpointOverride(URI.create(endpoint)).forcePathStyle(true);
        }
        return constructor.build();
    }
}
