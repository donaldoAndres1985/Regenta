package com.regenta.facturacion.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;

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
import com.regenta.facturacion.BaseDeFacturacion;
import com.regenta.facturacion.infra.ClienteDeLaDianStub;
import com.regenta.facturacion.infra.ConsumidorDeCierresFacturables;

/** HU-057. Modo de contingencia cuando la DIAN no responde. */
class GestionDeContingenciaTest extends BaseDeFacturacion {

    private static final Set<String> ADMIN = Set.of("FACTURACION_RESOLUCION_VER",
            "FACTURACION_RESOLUCION_EDITAR", "FACTURACION_FACTURA_VER", "FACTURACION_FACTURA_CREAR");
    private static final Set<String> INTERNO = Set.of();

    @Autowired
    private GestionDeContingencia contingencia;

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

    private void prepararNegocio(UUID negocio) {
        conDatosFiscales(negocio);
        enContexto(negocio, admin, ADMIN, () -> resoluciones.cargar(new SolicitudDeResolucion(null,
                "FACTURA_VENTA", "FV-" + negocio, "FE", 1, 100000, "clave",
                LocalDate.parse("2026-01-01"), LocalDate.parse("2030-01-01"), "PRODUCCION")));
        enContexto(negocio, admin, ADMIN, () -> firma.registrarCertificado(
                new SolicitudDeCertificado("cert", "AC", "SN", LocalDate.now().minusYears(1),
                        LocalDate.now().plusYears(1), "kms://" + negocio)));
    }

    private UUID emitirFactura(UUID negocio) {
        UUID ventaId = UUID.randomUUID();
        Map<String, Object> evento = Map.of("negocio_id", negocio.toString(),
                "venta_id", ventaId.toString(),
                "emisor", Map.of("x", "y"), "cliente", Map.of("x", "y"),
                "lineas", List.of(Map.of("descripcion", "Item", "cantidad", "1",
                        "precio_unitario", "10000")));
        try {
            MessageProperties props = new MessageProperties();
            props.setMessageId(UUID.randomUUID().toString());
            props.setReceivedRoutingKey("venta_completada");
            emisor.recibir(MessageBuilder.withBody(json.writeValueAsBytes(evento))
                    .andProperties(props).build());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        return UUID.fromString(comoElServicio(negocio,
                "select id from facturas where origen_id = '" + ventaId + "'").get(0));
    }

    private void abrirContingencia(UUID negocio) {
        enContexto(negocio, admin, INTERNO, (Runnable) () -> contingencia.abrirSiHaceFalta("prueba"));
    }

    @Test
    @DisplayName("Criterio 1: tras superar el umbral de intentos sin acuse se abre una contingencia y se avisa")
    void seAbreAlSuperarElUmbral() {
        prepararNegocio(negocioA);
        dian.disponible = false;
        UUID facturaId = emitirFactura(negocioA);

        for (int i = 0; i < 3; i++) {
            enContexto(negocioA, admin, INTERNO, () -> firma.firmarYTransmitir(facturaId));
        }

        List<String> abiertas = comoElServicio(negocioA,
                "select id::text from contingencias where fin_en is null");
        assertThat(abiertas).hasSize(1);
        // outbox_eventos no lleva RLS: se filtra por agregado_id porque otras
        // pruebas de la suite también abren contingencia.
        assertThat(comoElServicio(negocioA, "select count(*) from outbox_eventos where tipo_evento"
                + " = 'contingencia_abierta' and agregado_id = '" + abiertas.get(0) + "'"))
                .containsExactly("1");
    }

    @Test
    @DisplayName("Criterio 2: con contingencia abierta la factura se marca CONTINGENCIA y no se transmite")
    void enContingenciaSeMarcaYNoSeTransmite() {
        prepararNegocio(negocioA);
        abrirContingencia(negocioA);
        // La DIAN está arriba, pero la contingencia sigue abierta.
        UUID facturaId = emitirFactura(negocioA);

        enContexto(negocioA, admin, INTERNO, () -> firma.firmarYTransmitir(facturaId));

        assertThat(comoElServicio(negocioA, "select estado || '|' || (cufe is not null)"
                + " from facturas where id = '" + facturaId + "'"))
                .containsExactly("CONTINGENCIA|true");
        assertThat(comoElServicio(negocioA,
                "select count(*) from transmisiones where factura_id = '" + facturaId + "'"))
                .containsExactly("0");
    }

    @Test
    @DisplayName("Criterio 3: al cerrar la contingencia las facturas pendientes se transmiten en orden")
    void alCerrarSeRetransmite() {
        prepararNegocio(negocioA);
        abrirContingencia(negocioA);
        UUID f1 = emitirFactura(negocioA);
        UUID f2 = emitirFactura(negocioA);
        enContexto(negocioA, admin, INTERNO, () -> firma.firmarYTransmitir(f1));
        enContexto(negocioA, admin, INTERNO, () -> firma.firmarYTransmitir(f2));

        UUID contingenciaId = UUID.fromString(comoElServicio(negocioA,
                "select id from contingencias where fin_en is null").get(0));
        ContingenciaDelNegocio cerrada = enContexto(negocioA, admin, ADMIN,
                () -> firma.cerrarContingenciaYRetransmitir(contingenciaId));

        assertThat(cerrada.regularizada()).isTrue();
        assertThat(cerrada.abierta()).isFalse();
        assertThat(comoElServicio(negocioA, "select distinct estado from facturas"
                + " where id in ('" + f1 + "','" + f2 + "')")).containsExactly("ACEPTADA");
    }

    @Test
    @DisplayName("Criterio 4: la contingencia sabe cuántas facturas quedaron afectadas")
    void cuentaLasFacturasAfectadas() {
        prepararNegocio(negocioA);
        abrirContingencia(negocioA);
        for (int i = 0; i < 3; i++) {
            UUID f = emitirFactura(negocioA);
            enContexto(negocioA, admin, INTERNO, () -> firma.firmarYTransmitir(f));
        }

        UUID contingenciaId = UUID.fromString(comoElServicio(negocioA,
                "select id from contingencias where fin_en is null").get(0));
        ContingenciaDelNegocio vista = enContexto(negocioA, admin, ADMIN,
                () -> contingencia.ver(contingenciaId));

        assertThat(vista.facturasAfectadas()).isEqualTo(3);
    }

    @Test
    @DisplayName("Solo puede haber una contingencia abierta por negocio")
    void unaSolaAbierta() {
        prepararNegocio(negocioA);
        abrirContingencia(negocioA);
        abrirContingencia(negocioA);

        assertThat(comoElServicio(negocioA,
                "select count(*) from contingencias where fin_en is null")).containsExactly("1");
    }

    @Test
    @DisplayName("Las contingencias de un negocio no se ven desde otro")
    void aisladoPorNegocio() {
        prepararNegocio(negocioA);
        abrirContingencia(negocioA);

        assertThat(comoElServicio(negocioB, "select count(*) from contingencias"))
                .containsExactly("0");
    }
}
