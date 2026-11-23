package com.regenta.shared.almacen;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.regenta.shared.web.NoEncontradoException;

import software.amazon.awssdk.services.s3.S3Client;

/** HU-121 · El almacén contra un servidor S3 de verdad (la misma API que R2 o AWS en producción). */
@Testcontainers
class AlmacenS3EnContenedorTest {

    @Container
    static final GenericContainer<?> S3 = ServidorS3DePrueba.nuevo();

    private static S3Client s3;
    private static AlmacenS3 almacen;

    private final UUID negocioA = UUID.randomUUID();
    private final UUID negocioB = UUID.randomUUID();
    private final byte[] xml = "<Invoice>firmada</Invoice>".getBytes(StandardCharsets.UTF_8);

    @BeforeAll
    static void conectar() {
        s3 = ConfiguracionDelAlmacen.cliente(ServidorS3DePrueba.url(S3), "us-east-1", "prueba", "prueba");
        almacen = new AlmacenS3(s3, "regenta-documentos");
        almacen.asegurarBucket();
        almacen.asegurarBucket(); // la segunda vez ya existe y no falla
    }

    @Test
    @DisplayName("C1 · El objeto queda en el bucket, bajo la carpeta del negocio, y se lee por su URL")
    void guardarYLeer() {
        String url = almacen.guardar(negocioA, "facturas", "SETT1.xml", xml, "application/xml");

        assertThat(url).startsWith("s3://regenta-documentos/" + negocioA + "/facturas/").endsWith("/sett1.xml");
        String clave = url.substring("s3://regenta-documentos/".length());
        var objeto = s3.headObject(b -> b.bucket("regenta-documentos").key(clave));
        assertThat(objeto.contentType()).isEqualTo("application/xml");
        assertThat(objeto.metadata()).containsEntry("negocio", negocioA.toString());
        assertThat(almacen.leer(negocioA, url)).isEqualTo(xml);
    }

    @Test
    @DisplayName("C2 · Otro negocio no lee el objeto, y lo que no existe es 404, no un error del almacén")
    void otroNegocio() {
        String url = almacen.guardar(negocioA, "facturas", "SETT2.xml", xml, "application/xml");

        assertThatThrownBy(() -> almacen.leer(negocioB, url)).isInstanceOf(NoEncontradoException.class);
        assertThatThrownBy(() -> almacen.leer(negocioA, url.replace("sett2", "otro")))
                .isInstanceOf(NoEncontradoException.class);
    }

    @Test
    @DisplayName("C5 · Borrar lo quita del bucket")
    void borrar() {
        String url = almacen.guardar(negocioA, "reportes", "ventas.xlsx", new byte[] {1, 2}, "x");

        almacen.borrar(negocioA, url);

        assertThatThrownBy(() -> almacen.leer(negocioA, url)).isInstanceOf(NoEncontradoException.class);
    }

    @Test
    @DisplayName("C3 · Si el almacén no responde, avisa con AlmacenNoDisponibleException para reintentar")
    void caido() {
        AlmacenS3 sinServicio = new AlmacenS3(
                ConfiguracionDelAlmacen.cliente("http://127.0.0.1:1", "us-east-1", "x", "y"), "regenta-documentos");

        assertThatThrownBy(() -> sinServicio.guardar(negocioA, "facturas", "f.xml", xml, "application/xml"))
                .isInstanceOf(AlmacenNoDisponibleException.class);
    }
}
