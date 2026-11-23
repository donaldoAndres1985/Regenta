package com.regenta.shared.almacen;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

/**
 * HU-121 · Un servidor S3 de verdad para las pruebas: Adobe S3Mock, que habla la API de S3 como R2 o
 * AWS. (MinIO dejó de publicar sus imágenes.) No valida credenciales: lo de «solo el negocio dueño»
 * lo cubre la clave por negocio del almacén, y la privacidad del bucket la da el proveedor.
 */
final class ServidorS3DePrueba {

    static final int PUERTO = 9090;

    private ServidorS3DePrueba() {
    }

    @SuppressWarnings("resource")
    static GenericContainer<?> nuevo() {
        return new GenericContainer<>("adobe/s3mock:5.2.3")
                .withExposedPorts(PUERTO)
                .waitingFor(Wait.forListeningPort());
    }

    static String url(GenericContainer<?> s3) {
        return "http://" + s3.getHost() + ":" + s3.getMappedPort(PUERTO);
    }
}
