package com.regenta.facturacion.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.regenta.comun.errores.ReglaDeNegocioException;
import com.regenta.facturacion.BaseDeFacturacion;
import com.regenta.facturacion.infra.ConsumidorDeCierresFacturables;
import com.regenta.facturacion.infra.ConsumidorDeNegocio;

/**
 * Quién emite la factura (HU-115).
 *
 * <p>Los datos fiscales del negocio viven en {@code configuracion_negocio}, en
 * la base de servicio-usuarios, y Facturación no puede consultarla. Hasta ahora
 * {@code emisor_snapshot} se guardaba vacío: una factura sin emisor, que la
 * DIAN rechaza.
 */
class EmisorDeLaFacturaTest extends BaseDeFacturacion {

    private static final Set<String> DE_ADMIN = Set.of("FACTURACION_RESOLUCION_VER",
            "FACTURACION_RESOLUCION_EDITAR");
    private static final Set<String> VER = Set.of("FACTURACION_FACTURA_VER");

    @Autowired
    private ConsumidorDeNegocio consumidorDeNegocio;
    @Autowired
    private ConsumidorDeCierresFacturables consumidorDeCierres;
    @Autowired
    private GestionDeFacturas facturas;
    @Autowired
    private GestionDeResoluciones resoluciones;
    @Autowired
    private ObjectMapper json;

    private final UUID negocio = UUID.randomUUID();
    private final UUID admin = UUID.randomUUID();

    private void resolucionEnElNegocio() {
        enContexto(negocio, admin, DE_ADMIN, () -> resoluciones.cargar(new SolicitudDeResolucion(
                null, "FACTURA_VENTA", "R-" + negocio, "FE", 1, 100000, "clave-abc",
                LocalDate.parse("2026-01-01"), LocalDate.parse("2030-01-01"), "PRODUCCION")));
    }

    private void entregar(String tipoEvento, Map<String, Object> payload) {
        try {
            MessageProperties props = new MessageProperties();
            props.setMessageId(UUID.randomUUID().toString());
            props.setReceivedRoutingKey(tipoEvento);
            Message mensaje = MessageBuilder.withBody(json.writeValueAsBytes(payload))
                    .andProperties(props).build();
            if (tipoEvento.startsWith("negocio") || tipoEvento.startsWith("configuracion")) {
                consumidorDeNegocio.recibir(mensaje);
            } else {
                consumidorDeCierres.recibir(mensaje);
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private Map<String, Object> ventaCompletada(UUID ventaId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("negocio_id", negocio.toString());
        payload.put("venta_id", ventaId.toString());
        payload.put("numero", "FV-1");
        payload.put("lineas", List.of(Map.of(
                "descripcion", "Martillo",
                "cantidad", "1",
                "precio_unitario", "32000",
                "descuento_pct", "0",
                "impuestos", List.of(Map.of("codigo", "01", "nombre", "IVA",
                        "porcentaje", "19", "es_retencion", false)))));
        return payload;
    }

    private Map<String, Object> emisorDeLaUnicaFactura() {
        List<FacturaEmitida> lista = enContexto(negocio, admin, VER, () -> facturas.listar());
        assertThat(lista).hasSize(1);
        return enContexto(negocio, admin, VER, () -> facturas.ver(lista.get(0).id())).emisor();
    }

    @Test
    @DisplayName("Criterio 1: negocio_creado deja la copia local de los datos fiscales")
    void elAltaDejaLaCopiaLocal() {
        entregar("negocio_creado", datosDelNegocio());

        assertThat(comoElServicio(negocio,
                "select razon_social from emisor_negocio where negocio_id = '" + negocio + "'"))
                .containsExactly("Ferretería El Tornillo SAS");
    }

    @Test
    @DisplayName("Criterio 2: la factura sale con la identidad fiscal completa del emisor")
    void laFacturaLlevaElEmisorCompleto() {
        entregar("negocio_creado", datosDelNegocio());
        resolucionEnElNegocio();

        entregar("venta_completada", ventaCompletada(UUID.randomUUID()));

        assertThat(emisorDeLaUnicaFactura())
                .containsEntry("razon_social", "Ferretería El Tornillo SAS")
                .containsEntry("numero_documento", "900123456")
                .containsEntry("digito_verificacion", "8")
                .containsEntry("ciudad", "Medellín")
                .containsEntry("regimen_fiscal", "RESPONSABLE_IVA");
    }

    @Test
    @DisplayName("Criterio 3: al cambiar los datos fiscales, la factura nueva sale con los de hoy")
    void laFacturaNuevaLlevaLosDatosDeHoy() {
        entregar("negocio_creado", datosDelNegocio());
        resolucionEnElNegocio();
        entregar("venta_completada", ventaCompletada(UUID.randomUUID()));
        Map<String, Object> antes = emisorDeLaUnicaFactura();

        Map<String, Object> cambio = new HashMap<>(datosDelNegocio());
        cambio.put("razon_social", "Ferretería El Tornillo y Compañía SAS");
        entregar("configuracion_negocio_actualizada", cambio);
        entregar("venta_completada", ventaCompletada(UUID.randomUUID()));

        assertThat(antes).containsEntry("razon_social", "Ferretería El Tornillo SAS");
        List<FacturaEmitida> lista = enContexto(negocio, admin, VER, () -> facturas.listar());
        assertThat(lista).hasSize(2);
        List<String> razones = lista.stream()
                .map(f -> enContexto(negocio, admin, VER, () -> facturas.ver(f.id()))
                        .emisor().get("razon_social").toString())
                .toList();
        assertThat(razones)
                .as("la ya emitida conserva lo que decía; la nueva sale con lo de hoy")
                .containsExactlyInAnyOrder("Ferretería El Tornillo SAS",
                        "Ferretería El Tornillo y Compañía SAS");
    }

    @Test
    @DisplayName("Criterio 4: sin datos fiscales no se emite, y no se gasta un consecutivo")
    void sinDatosFiscalesNoSeEmiteNiSeGastaConsecutivo() {
        resolucionEnElNegocio();
        String antes = comoElServicio(negocio,
                "select consecutivo_actual from resoluciones where negocio_id = '" + negocio + "'").get(0);

        assertThatThrownBy(() -> entregar("venta_completada", ventaCompletada(UUID.randomUUID())))
                .hasRootCauseInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("fiscales");

        assertThat(comoElServicio(negocio,
                "select consecutivo_actual from resoluciones where negocio_id = '" + negocio + "'"))
                .as("un consecutivo gastado en una factura que no salió es un hueco en la numeración")
                .containsExactly(antes);
        assertThat(enContexto(negocio, admin, VER, () -> facturas.listar())).isEmpty();
    }

    @Test
    @DisplayName("Criterio 5: el municipio del adquiriente genérico es el del emisor")
    void elGenericoUsaElMunicipioDelEmisor() {
        entregar("negocio_creado", datosDelNegocio());
        resolucionEnElNegocio();

        entregar("venta_completada", ventaCompletada(UUID.randomUUID()));

        List<FacturaEmitida> lista = enContexto(negocio, admin, VER, () -> facturas.listar());
        Map<String, Object> adquiriente =
                enContexto(negocio, admin, VER, () -> facturas.ver(lista.get(0).id())).cliente();
        assertThat(adquiriente)
                .containsEntry("nombre", "Consumidor final")
                .containsEntry("ciudad", "Medellín");
    }

    @Test
    @DisplayName("Un negocio no ve los datos fiscales del otro")
    void aislamientoDelEmisor() {
        UUID otroNegocio = UUID.randomUUID();
        entregar("negocio_creado", datosDelNegocio());
        Map<String, Object> delOtro = new HashMap<>(datosDelNegocio());
        delOtro.put("negocio_id", otroNegocio.toString());
        delOtro.put("razon_social", "Droguería La Salud SAS");
        entregar("negocio_creado", delOtro);

        assertThat(comoElServicio(negocio, "select count(*) from emisor_negocio"))
                .containsExactly("1");
        assertThat(comoElServicio(otroNegocio,
                "select razon_social from emisor_negocio")).containsExactly("Droguería La Salud SAS");
    }

    private Map<String, Object> datosDelNegocio() {
        Map<String, Object> datos = new HashMap<>();
        datos.put("negocio_id", negocio.toString());
        datos.put("nombre_comercial", "Ferretería El Tornillo");
        datos.put("razon_social", "Ferretería El Tornillo SAS");
        datos.put("tipo_documento", "NIT");
        datos.put("numero_documento", "900123456");
        datos.put("digito_verificacion", "8");
        datos.put("direccion", "Calle 50 # 40-20");
        datos.put("ciudad", "Medellín");
        datos.put("departamento", "Antioquia");
        datos.put("codigo_postal", "050012");
        datos.put("pais", "CO");
        datos.put("regimen_fiscal", "RESPONSABLE_IVA");
        datos.put("responsabilidades_fiscales", List.of("O-13", "O-15"));
        return datos;
    }
}
