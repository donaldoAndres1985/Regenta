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
import com.regenta.facturacion.BaseDeFacturacion;
import com.regenta.facturacion.infra.ClienteDeLaDianStub;
import com.regenta.facturacion.infra.ConsumidorDeCierresFacturables;
import com.regenta.facturacion.infra.EnviadorDeCorreoStub;

/** HU-058 (backend). Detalle con motivo de rechazo y envío al cliente. */
class EnvioDeFacturaTest extends BaseDeFacturacion {

    private static final Set<String> ADMIN = Set.of("FACTURACION_RESOLUCION_VER",
            "FACTURACION_RESOLUCION_EDITAR", "FACTURACION_FACTURA_VER");
    private static final Set<String> INTERNO = Set.of();

    @Autowired
    private GestionDeFacturas facturas;

    @Autowired
    private GestionDeResoluciones resoluciones;

    @Autowired
    private GestionDeFirmaYTransmision firma;

    @Autowired
    private ConsumidorDeCierresFacturables emisor;

    @Autowired
    private ClienteDeLaDianStub dian;

    @Autowired
    private EnviadorDeCorreoStub correo;

    @Autowired
    private ObjectMapper json;

    private final UUID negocioA = UUID.randomUUID();
    private final UUID admin = UUID.randomUUID();

    @BeforeEach
    void limpiar() {
        dian.reiniciar();
    }

    private void preparar() {
        conDatosFiscales(negocioA);
        enContexto(negocioA, admin, ADMIN, () -> resoluciones.cargar(new SolicitudDeResolucion(null,
                "FACTURA_VENTA", "FV-" + negocioA, "FE", 1, 100000, "clave",
                LocalDate.parse("2026-01-01"), LocalDate.parse("2030-01-01"), "PRODUCCION")));
        enContexto(negocioA, admin, ADMIN, () -> firma.registrarCertificado(
                new SolicitudDeCertificado("cert", "AC", "SN", LocalDate.now().minusYears(1),
                        LocalDate.now().plusYears(1), "kms://" + negocioA)));
    }

    private UUID emitir(String correoCliente) {
        UUID ventaId = UUID.randomUUID();
        Map<String, Object> evento = Map.of("negocio_id", negocioA.toString(),
                "venta_id", ventaId.toString(),
                "emisor", Map.of("razon_social", "Mi Negocio"),
                "cliente", Map.of("razon_social", "Cliente SAS", "email", correoCliente),
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
        return UUID.fromString(comoElServicio(negocioA,
                "select id from facturas where origen_id = '" + ventaId + "'").get(0));
    }

    @Test
    @DisplayName("Criterio 4: el detalle de una factura rechazada trae el motivo del rechazo")
    void detalleDeRechazadaTraeElMotivo() {
        preparar();
        UUID facturaId = emitir("c@x.co");
        dian.aceptar = false;
        dian.codigoRechazo = "FAD09";
        dian.mensajeRechazo = "El adquiriente no existe en el RUT";
        enContexto(negocioA, admin, INTERNO, () -> firma.firmarYTransmitir(facturaId));

        FacturaDetalle d = enContexto(negocioA, admin, ADMIN, () -> facturas.ver(facturaId));
        assertThat(d.estado()).isEqualTo("RECHAZADA");
        assertThat(d.codigoRechazo()).isEqualTo("FAD09");
        assertThat(d.mensajeRechazo()).isEqualTo("El adquiriente no existe en el RUT");
        assertThat(d.cufe()).isNotBlank();
    }

    @Test
    @DisplayName("Criterio 2: una factura aceptada se envía al cliente con el PDF y el XML")
    void envioAlClienteAdjuntaPdfYXml() {
        preparar();
        UUID facturaId = emitir("cliente@correo.co");
        enContexto(negocioA, admin, INTERNO, () -> firma.firmarYTransmitir(facturaId));

        ResultadoDeEnvio r = enContexto(negocioA, admin, ADMIN,
                () -> facturas.enviarAlCliente(facturaId, "otro@correo.co"));

        assertThat(r.enviado()).isTrue();
        assertThat(r.destinatario()).isEqualTo("otro@correo.co");
        assertThat(r.adjuntos()).anyMatch(a -> a.startsWith("stub://"))     // XML
                .anyMatch(a -> a.startsWith("pdf:"));                       // PDF
        assertThat(correo.ultimoDestinatario).isEqualTo("otro@correo.co");
        assertThat(comoElServicio(negocioA, "select count(*) from transmisiones where factura_id = '"
                + facturaId + "' and evento = 'EMAIL_CLIENTE'")).containsExactly("1");
        assertThat(comoElServicio(negocioA, "select count(*) from outbox_eventos where tipo_evento"
                + " = 'factura_enviada_al_cliente' and agregado_id = '" + facturaId + "'"))
                .containsExactly("1");
    }

    @Test
    @DisplayName("Sin correo pedido se usa el del snapshot del cliente")
    void envioUsaElCorreoDelSnapshot() {
        preparar();
        UUID facturaId = emitir("delsnapshot@correo.co");
        enContexto(negocioA, admin, INTERNO, () -> firma.firmarYTransmitir(facturaId));

        ResultadoDeEnvio r = enContexto(negocioA, admin, ADMIN,
                () -> facturas.enviarAlCliente(facturaId, null));

        assertThat(r.destinatario()).isEqualTo("delsnapshot@correo.co");
    }

    @Test
    @DisplayName("Una factura que no está aceptada no se envía")
    void noSeEnviaSiNoEstaAceptada() {
        preparar();
        UUID facturaId = emitir("c@x.co");   // GENERADA, sin transmitir

        assertThatThrownBy(() -> enContexto(negocioA, admin, ADMIN,
                () -> facturas.enviarAlCliente(facturaId, "c@x.co")))
                .isInstanceOf(ReglaDeNegocioException.class);
    }
}
