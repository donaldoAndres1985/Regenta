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
import com.regenta.facturacion.infra.ConsumidorDeDevoluciones;

/** HU-056. Notas crédito. */
class GestionDeNotasCreditoTest extends BaseDeFacturacion {

    private static final Set<String> ADMIN = Set.of("FACTURACION_RESOLUCION_VER",
            "FACTURACION_RESOLUCION_EDITAR", "FACTURACION_FACTURA_VER", "FACTURACION_FACTURA_ANULAR");
    private static final Set<String> INTERNO = Set.of();

    @Autowired
    private GestionDeNotasCredito notas;

    @Autowired
    private GestionDeFacturas facturas;

    @Autowired
    private GestionDeResoluciones resoluciones;

    @Autowired
    private GestionDeFirmaYTransmision firma;

    @Autowired
    private ConsumidorDeCierresFacturables emisor;

    @Autowired
    private ConsumidorDeDevoluciones devoluciones;

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
        enContexto(negocio, admin, ADMIN, () -> {
            resoluciones.cargar(new SolicitudDeResolucion(null, "FACTURA_VENTA", "FV-" + negocio,
                    "FE", 1, 100000, "clave", LocalDate.parse("2026-01-01"),
                    LocalDate.parse("2030-01-01"), "PRODUCCION"));
            return resoluciones.cargar(new SolicitudDeResolucion(null, "NOTA_CREDITO",
                    "NC-" + negocio, "NC", 1, 100000, "clave", LocalDate.parse("2026-01-01"),
                    LocalDate.parse("2030-01-01"), "PRODUCCION"));
        });
        enContexto(negocio, admin, ADMIN, () -> firma.registrarCertificado(
                new SolicitudDeCertificado("cert", "AC", "SN", LocalDate.now().minusYears(1),
                        LocalDate.now().plusYears(1), "kms://" + negocio)));
    }

    private UUID facturaAceptada(UUID negocio, UUID ventaId) {
        Map<String, Object> evento = Map.of("negocio_id", negocio.toString(),
                "venta_id", ventaId.toString(), "numero", "FV-1",
                "emisor", Map.of("razon_social", "Mi Negocio"),
                "cliente", Map.of("razon_social", "Cliente SAS"),
                "lineas", List.of(Map.of("descripcion", "Item", "cantidad", "2",
                        "precio_unitario", "5000",
                        "impuestos", List.of(Map.of("codigo", "01", "nombre", "IVA",
                                "porcentaje", "19", "es_retencion", false)))));
        entregar(emisor::recibir, "venta_completada", UUID.randomUUID(), evento);
        UUID facturaId = UUID.fromString(comoElServicio(negocio,
                "select id from facturas where origen_id = '" + ventaId + "'").get(0));
        enContexto(negocio, admin, INTERNO, () -> firma.firmarYTransmitir(facturaId));
        return facturaId;
    }

    private void entregar(java.util.function.Consumer<Message> consumidor, String routingKey,
            UUID mensajeId, Map<String, Object> payload) {
        try {
            MessageProperties props = new MessageProperties();
            props.setMessageId(mensajeId.toString());
            props.setReceivedRoutingKey(routingKey);
            consumidor.accept(MessageBuilder.withBody(json.writeValueAsBytes(payload))
                    .andProperties(props).build());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    @DisplayName("Criterio 1: la NC queda referenciada a la factura origen con su código de motivo")
    void notaCreditoReferenciaLaFactura() {
        prepararNegocio(negocioA);
        UUID facturaId = facturaAceptada(negocioA, UUID.randomUUID());

        FacturaEmitida nc = enContexto(negocioA, admin, ADMIN, () -> notas.emitir(facturaId,
                new SolicitudDeNotaCredito("1", "devolución", List.of())));

        assertThat(nc.origenTipo()).isEqualTo("MANUAL");
        assertThat(comoElServicio(negocioA, "select tipo_documento || '|' || factura_origen_id"
                + " || '|' || codigo_nota from facturas where id = '" + nc.id() + "'"))
                .containsExactly("NOTA_CREDITO|" + facturaId + "|1");
    }

    @Test
    @DisplayName("Criterio 2: el CHECK de la base rechaza una NC sin factura de origen")
    void ncSinOrigenLaRechazaLaBase() {
        prepararNegocio(negocioA);
        UUID facturaId = facturaAceptada(negocioA, UUID.randomUUID());
        UUID ncId = enContexto(negocioA, admin, ADMIN, () -> notas.emitir(facturaId,
                new SolicitudDeNotaCredito("1", "x", List.of()))).id();

        assertThatThrownBy(() -> ejecutarComoElServicio(negocioA,
                "update facturas set factura_origen_id = null where id = '" + ncId + "'"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Criterio 3: devolucion_registrada emite la NC por lo devuelto, una sola vez")
    void notaCreditoDesdeDevolucion() {
        prepararNegocio(negocioA);
        UUID ventaId = UUID.randomUUID();
        facturaAceptada(negocioA, ventaId);
        UUID devolucionId = UUID.randomUUID();
        Map<String, Object> evento = Map.of("negocio_id", negocioA.toString(),
                "venta_id", ventaId.toString(), "devolucion_id", devolucionId.toString(),
                "motivo", "DEFECTUOSO",
                "lineas", List.of(Map.of("descripcion", "Item", "cantidad", "1",
                        "precio_unitario", "5000",
                        "impuestos", List.of(Map.of("codigo", "01", "nombre", "IVA",
                                "porcentaje", "19", "es_retencion", false)))));

        UUID mensajeId = UUID.randomUUID();
        entregar(devoluciones::recibir, "devolucion_registrada", mensajeId, evento);
        entregar(devoluciones::recibir, "devolucion_registrada", mensajeId, evento);        // reentrega
        entregar(devoluciones::recibir, "devolucion_registrada", UUID.randomUUID(), evento); // otro id

        assertThat(comoElServicio(negocioA, "select count(*) from facturas where tipo_documento"
                + " = 'NOTA_CREDITO' and origen_id = '" + devolucionId + "'")).containsExactly("1");
        assertThat(comoElServicio(negocioA, "select total from facturas where tipo_documento"
                + " = 'NOTA_CREDITO' and origen_id = '" + devolucionId + "'"))
                .containsExactly("5950.0000");
    }

    @Test
    @DisplayName("Criterio 4: al consultar la factura origen se ven sus notas crédito")
    void laFacturaOrigenSeVeEnlazada() {
        prepararNegocio(negocioA);
        UUID facturaId = facturaAceptada(negocioA, UUID.randomUUID());
        FacturaEmitida nc = enContexto(negocioA, admin, ADMIN, () -> notas.emitir(facturaId,
                new SolicitudDeNotaCredito("1", "x", List.of())));

        FacturaDetalle detalle = enContexto(negocioA, admin, ADMIN, () -> facturas.ver(facturaId));
        assertThat(detalle.notasCredito()).extracting(NotaCreditoEnlazada::id)
                .containsExactly(nc.id());
        assertThat(detalle.notasCredito().get(0).codigoNota()).isEqualTo("1");
    }

    @Test
    @DisplayName("Solo se emite NC de una factura aceptada")
    void soloDeFacturaAceptada() {
        prepararNegocio(negocioA);
        // Factura emitida pero sin aceptar: la firma la deja GENERADA->... la
        // dejamos sin transmitir usando la DIAN caída.
        dian.disponible = false;
        UUID ventaId = UUID.randomUUID();
        Map<String, Object> evento = Map.of("negocio_id", negocioA.toString(),
                "venta_id", ventaId.toString(),
                "emisor", Map.of("x", "y"), "cliente", Map.of("x", "y"),
                "lineas", List.of(Map.of("descripcion", "I", "cantidad", "1",
                        "precio_unitario", "1000")));
        entregar(emisor::recibir, "venta_completada", UUID.randomUUID(), evento);
        UUID facturaId = UUID.fromString(comoElServicio(negocioA,
                "select id from facturas where origen_id = '" + ventaId + "'").get(0));
        enContexto(negocioA, admin, INTERNO, () -> firma.firmarYTransmitir(facturaId));

        assertThatThrownBy(() -> enContexto(negocioA, admin, ADMIN, () -> notas.emitir(facturaId,
                new SolicitudDeNotaCredito("1", "x", List.of()))))
                .isInstanceOf(ReglaDeNegocioException.class);
    }

    @Test
    @DisplayName("Emitir una NC exige FACTURACION_FACTURA_ANULAR")
    void permiso() {
        prepararNegocio(negocioA);
        UUID facturaId = facturaAceptada(negocioA, UUID.randomUUID());

        assertThatThrownBy(() -> enContexto(negocioA, admin, Set.of("FACTURACION_FACTURA_VER"),
                () -> notas.emitir(facturaId, new SolicitudDeNotaCredito("1", "x", List.of()))))
                .isInstanceOf(SinPermisoException.class);
    }

    @Test
    @DisplayName("Las notas crédito de un negocio no se ven desde otro")
    void aisladoPorNegocio() {
        prepararNegocio(negocioA);
        UUID facturaId = facturaAceptada(negocioA, UUID.randomUUID());
        enContexto(negocioA, admin, ADMIN, () -> notas.emitir(facturaId,
                new SolicitudDeNotaCredito("1", "x", List.of())));

        assertThat(comoElServicio(negocioB,
                "select count(*) from facturas where tipo_documento = 'NOTA_CREDITO'"))
                .containsExactly("0");
    }
}
