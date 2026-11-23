package com.regenta.shared.almacen;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.regenta.PostgresDePrueba;
import com.regenta.facturacion.infrastructure.AlmacenDeDocumentosEnObjetos;
import com.regenta.reportes.application.ArchivosDeReporte;
import com.regenta.shared.tenancy.ContextoDeNegocio;
import com.regenta.shared.tenancy.DatosDelNegocio;
import com.regenta.shared.web.RecursoExpiradoException;

/**
 * HU-121 de punta a punta: PostgreSQL (facturacion V6, reportes V8, con RLS) y MinIO. En la base
 * queda la URL y no el contenido (C1); con el almacén caído la factura no se pierde (C3); los archivos
 * que estaban en la base se suben (C4) y los vencidos se borran y quedan expirados (C5).
 */
@DataJpaTest
@Testcontainers
class DocumentosEnElAlmacenEnLaBaseTest extends PostgresDePrueba {

    @Container
    static final MinIOContainer MINIO = new MinIOContainer("minio/minio:RELEASE.2023-09-04T19-57-37Z");

    private static AlmacenS3 almacen;

    @Autowired
    private JdbcTemplate jdbc;

    private final UUID negocio = UUID.randomUUID();
    private final byte[] xml = "<Invoice>firmada</Invoice>".getBytes(StandardCharsets.UTF_8);

    @BeforeAll
    static void conectar() {
        almacen = new AlmacenS3(ConfiguracionDelAlmacen.cliente(MINIO.getS3URL(), "us-east-1", MINIO.getUserName(),
                MINIO.getPassword()), "regenta-documentos");
        almacen.asegurarBucket();
    }

    @AfterEach
    void limpiar() {
        ContextoDeNegocio.limpiar();
    }

    private void comoSistema() {
        comoElNegocio(jdbc, negocio);
        ContextoDeNegocio.establecer(new DatosDelNegocio(negocio, null, "", "", Set.of("SISTEMA"),
                Set.of("FACTURACION", "REPORTES"), Set.of(), Set.of()));
    }

    /** Una factura firmada cuyo XML quedó «por subir». */
    private UUID facturaCon(String xmlUrl) {
        UUID resolucion = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO facturacion.resoluciones (id, negocio_id, tipo_documento, numero_resolucion, prefijo,
                    rango_desde, rango_hasta, consecutivo_actual, vigente_desde, vigente_hasta)
                VALUES (?, ?, 'FACTURA_VENTA', '18760000001', 'SETT', 1, 5000, 2, current_date, current_date + 365)
                """, resolucion, negocio);
        UUID factura = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO facturacion.facturas (id, negocio_id, resolucion_id, tipo_documento, prefijo, numero,
                    origen_tipo, emisor_snapshot, cliente_snapshot, estado, cufe, xml_url)
                VALUES (?, ?, ?, 'FACTURA_VENTA', 'SETT', 1, 'MANUAL', '{}'::jsonb, '{}'::jsonb, 'FIRMADA', 'cufe-1', ?)
                """, factura, negocio, resolucion, xmlUrl);
        return factura;
    }

    @Test
    @DisplayName("C3 · Con el almacén caído el XML espera en la base; al volver se sube y la factura queda con su URL")
    void facturaConElAlmacenCaido() {
        comoSistema();
        AlmacenS3 caido = new AlmacenS3(ConfiguracionDelAlmacen.cliente("http://127.0.0.1:1", "us-east-1", "x", "y"),
                "regenta-documentos");
        String provisional = new AlmacenDeDocumentosEnObjetos(caido, jdbc).guardar(negocio, "SETT1.xml", xml,
                "application/xml");
        assertThat(provisional).startsWith("pendiente://");
        UUID factura = facturaCon(provisional);

        AlmacenDeDocumentosEnObjetos documentos = new AlmacenDeDocumentosEnObjetos(almacen, jdbc);
        // La transmisión puede leerlo mientras espera.
        assertThat(documentos.leer(negocio, provisional)).isEqualTo(xml);

        jdbc.update("UPDATE facturacion.documentos_por_subir SET proximo_intento_en = now() WHERE negocio_id = ?",
                negocio);
        assertThat(documentos.subirPendientes()).isEqualTo(1);

        String url = jdbc.queryForObject("SELECT xml_url FROM facturacion.facturas WHERE id = ?", String.class, factura);
        assertThat(url).startsWith("s3://regenta-documentos/" + negocio + "/facturas/");
        assertThat(documentos.leer(negocio, url)).isEqualTo(xml);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM facturacion.documentos_por_subir WHERE negocio_id = ?",
                Integer.class, negocio)).isZero();
    }

    private UUID ejecucion() {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO reportes.ejecuciones_reporte (id, negocio_id, codigo, formato, estado) "
                + "VALUES (?, ?, 'ventas-por-dia', 'XLSX', 'COMPLETADO')", id, negocio);
        return id;
    }

    @Test
    @DisplayName("C1 · El archivo exportado queda en el almacén y en la base solo su URL")
    void reporteEnElAlmacen() {
        comoSistema();
        UUID ejecucion = ejecucion();
        ArchivosDeReporte archivos = new ArchivosDeReporte(jdbc, almacen, Duration.ofDays(30));

        archivos.guardar(negocio, ejecucion, "ventas.xlsx", "application/xlsx", new byte[] {80, 75, 3, 4});

        var fila = jdbc.queryForMap("SELECT contenido, url, expira_en FROM reportes.archivos_reporte WHERE ejecucion_id = ?",
                ejecucion);
        assertThat(fila.get("contenido")).isNull();
        assertThat((String) fila.get("url")).startsWith("s3://regenta-documentos/" + negocio + "/reportes/");
        assertThat(fila.get("expira_en")).isNotNull();
        assertThat(archivos.leer(ejecucion).contenido()).containsExactly(80, 75, 3, 4);
    }

    @Test
    @DisplayName("C4 / C5 · Los que estaban en la base se suben y la columna queda vacía; al vencer, se borran y expiran")
    void migrarYVencer() {
        comoSistema();
        UUID ejecucion = ejecucion();
        jdbc.update("""
                INSERT INTO reportes.archivos_reporte (ejecucion_id, negocio_id, nombre, tipo_mime, bytes, contenido,
                    creado_en)
                VALUES (?, ?, 'ventas.xlsx', 'application/xlsx', 4, ?, now() - interval '40 days')
                """, ejecucion, negocio, new byte[] {80, 75, 3, 4});
        ArchivosDeReporte archivos = new ArchivosDeReporte(jdbc, almacen, Duration.ofDays(30));

        assertThat(archivos.subirLosDeLaBase()).isEqualTo(1);
        var subido = jdbc.queryForMap("SELECT contenido, url FROM reportes.archivos_reporte WHERE ejecucion_id = ?",
                ejecucion);
        assertThat(subido.get("contenido")).isNull();
        String url = (String) subido.get("url");
        assertThat(almacen.leer(negocio, url)).containsExactly(80, 75, 3, 4);

        // Creado hace 40 días con retención de 30: ya venció.
        assertThat(archivos.borrarVencidos()).isEqualTo(1);
        var vencido = jdbc.queryForMap("SELECT url, expirado_en FROM reportes.archivos_reporte WHERE ejecucion_id = ?",
                ejecucion);
        assertThat(vencido.get("url")).isNull();
        assertThat(vencido.get("expirado_en")).isNotNull();
        assertThat(jdbc.queryForObject("SELECT estado FROM reportes.ejecuciones_reporte WHERE id = ?", String.class,
                ejecucion)).isEqualTo("COMPLETADO");
        assertThatThrownBy(() -> archivos.leer(ejecucion)).isInstanceOf(RecursoExpiradoException.class);
        assertThatThrownBy(() -> almacen.leer(negocio, url))
                .isInstanceOf(com.regenta.shared.web.NoEncontradoException.class);
    }
}
