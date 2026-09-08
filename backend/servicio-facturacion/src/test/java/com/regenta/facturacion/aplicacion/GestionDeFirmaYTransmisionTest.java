package com.regenta.facturacion.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.regenta.comun.errores.ReglaDeNegocioException;
import com.regenta.comun.errores.SinPermisoException;
import com.regenta.facturacion.BaseDeFacturacion;
import com.regenta.facturacion.infra.ClienteDeLaDianStub;
import com.regenta.facturacion.infra.ConsumidorDeCierresFacturables;

/** HU-055. Firma digital y transmisión a la DIAN. */
class GestionDeFirmaYTransmisionTest extends BaseDeFacturacion {

    private static final Set<String> ADMIN = Set.of("FACTURACION_RESOLUCION_VER",
            "FACTURACION_RESOLUCION_EDITAR", "FACTURACION_FACTURA_VER");
    private static final Set<String> INTERNO = Set.of();

    @Autowired
    private GestionDeFirmaYTransmision firma;

    @Autowired
    private GestionDeResoluciones resoluciones;

    @Autowired
    private ConsumidorDeCierresFacturables emisor;

    @Autowired
    private ClienteDeLaDianStub dian;

    @Autowired
    private ObjectMapper json;

    private final UUID negocioA = UUID.randomUUID();
    private final UUID negocioB = UUID.randomUUID();
    private final UUID admin = UUID.randomUUID();

    @BeforeEach
    void limpiar() {
        dian.reiniciar();
    }

    private void resolucionEn(UUID negocio) {
        enContexto(negocio, admin, ADMIN, () -> resoluciones.cargar(new SolicitudDeResolucion(
                null, "FACTURA_VENTA", "R-" + negocio, "FE", 1, 100000, "clave-abc",
                LocalDate.parse("2026-01-01"), LocalDate.parse("2030-01-01"), "PRODUCCION")));
    }

    private void certificadoEn(UUID negocio, LocalDate vigenteHasta) {
        enContexto(negocio, admin, ADMIN, () -> firma.registrarCertificado(
                new SolicitudDeCertificado("cert-" + vigenteHasta, "AC Digital", "SN-1",
                        LocalDate.now().minusYears(1), vigenteHasta, "kms://" + negocio)));
    }

    private UUID emitirFactura(UUID negocio) {
        Map<String, Object> evento = Map.of(
                "negocio_id", negocio.toString(),
                "venta_id", UUID.randomUUID().toString(),
                "numero", "FV-1",
                "emisor", Map.of("razon_social", "Mi Negocio SAS"),
                "cliente", Map.of("razon_social", "Cliente SAS"),
                "lineas", List.of(Map.of("descripcion", "Item", "cantidad", "1",
                        "precio_unitario", "10000",
                        "impuestos", List.of(Map.of("codigo", "01", "nombre", "IVA",
                                "porcentaje", "19", "es_retencion", false)))));
        try {
            MessageProperties props = new MessageProperties();
            props.setMessageId(UUID.randomUUID().toString());
            props.setReceivedRoutingKey("venta_completada");
            Message m = MessageBuilder.withBody(json.writeValueAsBytes(evento))
                    .andProperties(props).build();
            emisor.recibir(m);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        return UUID.fromString(comoElServicio(negocio, "select id from facturas limit 1").get(0));
    }

    private ResultadoDeTransmision transmitir(UUID negocio, UUID facturaId) {
        return enContexto(negocio, admin, INTERNO, () -> firma.firmarYTransmitir(facturaId));
    }

    @Test
    @DisplayName("Criterio 1: al firmar se calcula el CUFE y el XML va a storage, no a la base")
    void firmaCalculaCufeYGuardaFuera() {
        resolucionEn(negocioA);
        certificadoEn(negocioA, LocalDate.now().plusYears(1));
        UUID facturaId = emitirFactura(negocioA);

        enContexto(negocioA, admin, INTERNO, (Runnable) () -> firma.firmar(facturaId));

        assertThat(comoElServicio(negocioA, "select estado || '|' || (cufe is not null) || '|'"
                + " || (xml_url like 'stub://%') from facturas where id = '" + facturaId + "'"))
                .containsExactly("FIRMADA|true|true");
        assertThat(comoElServicio(negocioA,
                "select length(cufe) >= 40 from facturas where id = '" + facturaId + "'"))
                .containsExactly("t");
    }

    @Test
    @DisplayName("Criterio 2: la transmisión aceptada deja el request y el response en transmisiones")
    void transmisionAceptadaSeRegistra() {
        resolucionEn(negocioA);
        certificadoEn(negocioA, LocalDate.now().plusYears(1));
        UUID facturaId = emitirFactura(negocioA);

        ResultadoDeTransmision r = transmitir(negocioA, facturaId);

        assertThat(r.aceptada()).isTrue();
        assertThat(r.estado()).isEqualTo("ACEPTADA");
        assertThat(comoElServicio(negocioA, "select count(*) from transmisiones where factura_id = '"
                + facturaId + "' and request is not null and response is not null"))
                .containsExactly("1");
    }

    @Test
    @DisplayName("Criterio 3: un rechazo deja la factura RECHAZADA con el código y publica una alerta")
    void rechazoDejaConstanciaYAlerta() {
        resolucionEn(negocioA);
        certificadoEn(negocioA, LocalDate.now().plusYears(1));
        UUID facturaId = emitirFactura(negocioA);
        dian.aceptar = false;
        dian.codigoRechazo = "FAD09";

        ResultadoDeTransmision r = transmitir(negocioA, facturaId);

        assertThat(r.estado()).isEqualTo("RECHAZADA");
        assertThat(r.codigoError()).isEqualTo("FAD09");
        assertThat(comoElServicio(negocioA, "select respuesta_dian ->> 'codigo' from facturas"
                + " where id = '" + facturaId + "'")).containsExactly("FAD09");
        assertThat(comoElServicio(negocioA, "select count(*) from outbox_eventos where tipo_evento"
                + " = 'factura_rechazada' and agregado_id = '" + facturaId + "'"))
                .containsExactly("1");
    }

    @Test
    @DisplayName("Criterio 4: si la DIAN no responde la factura no se pierde y el barrido la reintenta")
    void sinRespuestaSeReintenta() {
        resolucionEn(negocioA);
        certificadoEn(negocioA, LocalDate.now().plusYears(1));
        UUID facturaId = emitirFactura(negocioA);
        dian.disponible = false;

        ResultadoDeTransmision primer = transmitir(negocioA, facturaId);
        assertThat(primer.estado()).isEqualTo("ENVIADA");
        assertThat(primer.intentos()).isEqualTo(1);
        assertThat(comoElServicio(negocioA, "select count(*) from transmisiones where factura_id = '"
                + facturaId + "' and codigo_error = 'SIN_RESPUESTA'")).containsExactly("1");

        dian.disponible = true;
        int reintentadas = enContexto(negocioA, admin, INTERNO, () -> firma.reintentarPendientes());
        assertThat(reintentadas).isEqualTo(1);
        assertThat(comoElServicio(negocioA, "select estado || '|' || intentos_envio from facturas"
                + " where id = '" + facturaId + "'")).containsExactly("ACEPTADA|2");
    }

    @Test
    @DisplayName("Criterio 5: un certificado a menos de 30 días de vencer genera una alerta")
    void certificadoPorVencer() {
        certificadoEn(negocioA, LocalDate.now().plusDays(20));
        certificadoEn(negocioA, LocalDate.now().plusDays(90));

        int avisados = enContexto(negocioA, admin, INTERNO,
                () -> firma.revisarCertificadosPorVencer());

        assertThat(avisados).isEqualTo(1);
        assertThat(comoElServicio(negocioA, "select count(*) from outbox_eventos"
                + " where tipo_evento = 'certificado_por_vencer'")).containsExactly("1");
    }

    @Test
    @DisplayName("Criterio 6: el certificado se guarda por referencia; sin ella es 422")
    void soloLaReferenciaDelCertificado() {
        assertThatThrownBy(() -> enContexto(negocioA, admin, ADMIN, () -> firma.registrarCertificado(
                new SolicitudDeCertificado("c", "AC", "SN", LocalDate.now(),
                        LocalDate.now().plusYears(1), "  "))))
                .isInstanceOf(ReglaDeNegocioException.class);

        certificadoEn(negocioA, LocalDate.now().plusYears(1));
        assertThat(comoElServicio(negocioA, "select referencia_kms from certificados limit 1"))
                .containsExactly("kms://" + negocioA);
        // La tabla no tiene columna para el archivo: es estructural.
        assertThat(comoElServicio(negocioA, "select count(*) from information_schema.columns"
                + " where table_schema='facturacion' and table_name='certificados'"
                + " and column_name in ('p12','archivo','contenido','clave_privada')"))
                .containsExactly("0");
    }

    @Test
    @DisplayName("Sin certificado activo, firmar falla")
    void sinCertificado() {
        resolucionEn(negocioA);
        UUID facturaId = emitirFactura(negocioA);

        assertThatThrownBy(() -> enContexto(negocioA, admin, INTERNO,
                (Runnable) () -> firma.firmar(facturaId)))
                .isInstanceOf(ReglaDeNegocioException.class);
    }

    @Test
    @DisplayName("Registrar un certificado exige FACTURACION_RESOLUCION_EDITAR")
    void permisoParaCertificado() {
        assertThatThrownBy(() -> enContexto(negocioA, admin, Set.of("FACTURACION_RESOLUCION_VER"),
                () -> firma.registrarCertificado(new SolicitudDeCertificado("c", "AC", "SN",
                        LocalDate.now(), LocalDate.now().plusYears(1), "kms://x"))))
                .isInstanceOf(SinPermisoException.class);
    }

    @Test
    @DisplayName("Los certificados y transmisiones de un negocio no se ven desde otro")
    void aisladoPorNegocio() {
        resolucionEn(negocioA);
        certificadoEn(negocioA, LocalDate.now().plusYears(1));
        UUID facturaId = emitirFactura(negocioA);
        transmitir(negocioA, facturaId);

        assertThat(comoElServicio(negocioB, "select count(*) from certificados"))
                .containsExactly("0");
        assertThat(comoElServicio(negocioB, "select count(*) from transmisiones"))
                .containsExactly("0");
    }
}
