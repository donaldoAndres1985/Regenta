package com.regenta.facturacion.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import com.regenta.shared.almacen.AlmacenEnMemoria;
import com.regenta.shared.tenancy.ContextoDeNegocio;
import com.regenta.shared.tenancy.DatosDelNegocio;

/** HU-121 · El XML firmado en el almacén, y C3: con el almacén caído la emisión no se pierde. */
class AlmacenDeDocumentosEnObjetosTest {

    private final UUID negocio = UUID.randomUUID();
    private final AlmacenEnMemoria almacen = new AlmacenEnMemoria();
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final AlmacenDeDocumentosEnObjetos documentos = new AlmacenDeDocumentosEnObjetos(almacen, jdbc);
    private final byte[] xml = "<Invoice>firmada</Invoice>".getBytes(StandardCharsets.UTF_8);

    @BeforeEach
    void contexto() {
        ContextoDeNegocio.establecer(new DatosDelNegocio(negocio, null, "", "", Set.of("SISTEMA"), Set.of("FACTURACION"),
                Set.of("FACTURACION_FACTURA_CREAR"), Set.of()));
    }

    @AfterEach
    void limpiar() {
        ContextoDeNegocio.limpiar();
    }

    @Test
    @DisplayName("C1 · El XML va al almacén y en la factura solo queda su URL")
    void alAlmacen() {
        String url = documentos.guardar(negocio, "SETT1.xml", xml, "application/xml");

        assertThat(url).startsWith("memoria://regenta/" + negocio + "/facturas/");
        assertThat(documentos.leer(negocio, url)).isEqualTo(xml);
        verify(jdbc, never()).update(contains("documentos_por_subir"), any(Object[].class));
    }

    @Test
    @DisplayName("C3 · Con el almacén caído, el XML espera en la base con una URL provisional y se puede leer igual")
    void almacenCaido() {
        almacen.simularCaida(true);

        String url = documentos.guardar(negocio, "SETT1.xml", xml, "application/xml");

        assertThat(url).startsWith("pendiente://");
        ArgumentCaptor<Object[]> args = ArgumentCaptor.forClass(Object[].class);
        verify(jdbc).update(contains("INSERT INTO facturacion.documentos_por_subir"), args.capture());
        assertThat(args.getValue()[1]).isEqualTo(negocio);
        assertThat(args.getValue()[4]).isEqualTo(xml);

        UUID id = UUID.fromString(url.substring("pendiente://".length()));
        when(jdbc.query(anyString(), any(RowMapper.class), eq(id), eq(negocio))).thenReturn(List.of(xml));
        assertThat(documentos.leer(negocio, url)).isEqualTo(xml);
    }

    @Test
    @DisplayName("C3 · Cuando el almacén vuelve, el barrido lo sube, cambia la URL de la factura y lo saca de la espera")
    void subirAlVolver() {
        UUID id = UUID.randomUUID();
        Map<String, Object> pendiente = new HashMap<>(Map.of("id", id, "nombre", "SETT1.xml", "tipo_mime",
                "application/xml", "contenido", xml, "intentos", 1));
        when(jdbc.queryForList(contains("documentos_por_subir"), eq(negocio))).thenReturn(List.of(pendiente));

        assertThat(documentos.subirPendientes()).isEqualTo(1);

        ArgumentCaptor<Object[]> args = ArgumentCaptor.forClass(Object[].class);
        verify(jdbc).update(contains("SET xml_url = ?"), args.capture());
        assertThat((String) args.getValue()[0]).startsWith("memoria://regenta/" + negocio + "/facturas/");
        assertThat(args.getValue()[2]).isEqualTo("pendiente://" + id);
        verify(jdbc).update(contains("DELETE FROM facturacion.documentos_por_subir"), eq(id));
        assertThat(almacen.cuantos()).isEqualTo(1);
    }

    @Test
    @DisplayName("C3 · Si sigue caído, se reintenta más tarde y no se borra nada")
    void sigueCaido() {
        almacen.simularCaida(true);
        UUID id = UUID.randomUUID();
        Map<String, Object> pendiente = new HashMap<>(Map.of("id", id, "nombre", "SETT1.xml", "tipo_mime",
                "application/xml", "contenido", xml, "intentos", 2));
        when(jdbc.queryForList(contains("documentos_por_subir"), eq(negocio))).thenReturn(List.of(pendiente));

        assertThat(documentos.subirPendientes()).isZero();

        verify(jdbc).update(contains("SET intentos = ?"), eq(3), anyString(), eq("9"), eq(id));
        verify(jdbc, never()).update(contains("DELETE"), any(Object[].class));
    }
}
