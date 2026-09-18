package com.regenta.facturacion.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;

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
import com.regenta.facturacion.BaseDeFacturacion;
import com.regenta.facturacion.infra.ConsumidorDeCierresFacturables;

/**
 * Quién es el adquiriente cuando la venta fue a consumidor final
 * (`design/comportamiento/ClienteVenta.md`, R8 y R9).
 *
 * <p>{@code facturas.cliente_snapshot} es NOT NULL y la mayoría de las ventas
 * de mostrador no llevan cliente. Emitir con el adquiriente vacío no es una
 * factura incompleta: es una factura que la DIAN rechaza.
 */
class AdquirienteGenericoTest extends BaseDeFacturacion {

    private static final Set<String> DE_ADMIN = Set.of("FACTURACION_RESOLUCION_VER",
            "FACTURACION_RESOLUCION_EDITAR");
    private static final Set<String> VER = Set.of("FACTURACION_FACTURA_VER");

    @Autowired
    private ConsumidorDeCierresFacturables consumidor;
    @Autowired
    private GestionDeFacturas facturas;
    @Autowired
    private GestionDeResoluciones resoluciones;
    @Autowired
    private ObjectMapper json;

    private final UUID negocio = UUID.randomUUID();
    private final UUID admin = UUID.randomUUID();

    private void resolucionEnElNegocio() {
        conDatosFiscales(negocio);
        enContexto(negocio, admin, DE_ADMIN, () -> resoluciones.cargar(new SolicitudDeResolucion(
                null, "FACTURA_VENTA", "R-" + negocio, "FE", 1, 100000, "clave-abc",
                LocalDate.parse("2026-01-01"), LocalDate.parse("2030-01-01"), "PRODUCCION")));
    }

    private static Map<String, Object> linea() {
        return Map.of(
                "descripcion", "Martillo",
                "cantidad", "1",
                "precio_unitario", "32000",
                "descuento_pct", "0",
                "impuestos", List.of(Map.of(
                        "codigo", "01", "nombre", "IVA", "porcentaje", "19", "es_retencion", false)));
    }

    /** Una venta cerrada, con o sin cliente según lo que se le pase. */
    private UUID ventaCompletada(Object clienteId, Object snapshot) {
        UUID ventaId = UUID.randomUUID();
        Map<String, Object> payload = new HashMap<>();
        payload.put("negocio_id", negocio.toString());
        payload.put("venta_id", ventaId.toString());
        payload.put("numero", "FV-1");
        payload.put("cliente_id", clienteId);
        payload.put("cliente_snapshot", snapshot);
        payload.put("emisor", Map.of("razon_social", "Mi Negocio SAS", "nit", "900111222"));
        payload.put("lineas", List.of(linea()));
        try {
            MessageProperties props = new MessageProperties();
            props.setMessageId(UUID.randomUUID().toString());
            props.setReceivedRoutingKey("venta_completada");
            Message mensaje = MessageBuilder.withBody(json.writeValueAsBytes(payload))
                    .andProperties(props).build();
            consumidor.recibir(mensaje);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        return ventaId;
    }

    private Map<String, Object> adquirienteDeLaUnicaFactura() {
        List<FacturaEmitida> lista = enContexto(negocio, admin, VER, () -> facturas.listar());
        assertThat(lista).hasSize(1);
        return enContexto(negocio, admin, VER, () -> facturas.ver(lista.get(0).id())).cliente();
    }

    @Test
    @DisplayName("R8: una venta sin cliente se factura al adquiriente genérico de la DIAN")
    void sinClienteSaleElGenerico() {
        resolucionEnElNegocio();

        ventaCompletada(null, null);

        assertThat(adquirienteDeLaUnicaFactura())
                .containsEntry("nombre", "Consumidor final")
                .containsEntry("tipo_documento", "13")
                .containsEntry("numero_documento", "222222222222")
                .containsEntry("tipo_organizacion", "2")
                .containsEntry("responsabilidad_fiscal", "R-99-PN")
                .containsEntry("pais", "CO");
    }

    @Test
    @DisplayName("R9: con cliente se factura al que la venta congeló, no al genérico")
    void conClienteSaleElDeLaVenta() {
        resolucionEnElNegocio();
        UUID cliente = UUID.randomUUID();

        ventaCompletada(cliente.toString(), Map.of(
            "cliente_id", cliente.toString(),
            "nombre", "Materiales Cruz S.A.S.",
            "tipo_documento", "NIT",
            "numero_documento", "900412883",
            "digito_verificacion", "1"));

        assertThat(adquirienteDeLaUnicaFactura())
                .containsEntry("nombre", "Materiales Cruz S.A.S.")
                .containsEntry("numero_documento", "900412883");
    }

    @Test
    @DisplayName("El snapshot que llega como texto JSON tampoco se pierde")
    void elSnapshotComoTextoSeEntiende() {
        resolucionEnElNegocio();
        UUID cliente = UUID.randomUUID();

        ventaCompletada(cliente.toString(),
                "{\"nombre\":\"Ferretería El Roble\",\"tipo_documento\":\"NIT\","
                        + "\"numero_documento\":\"901115220\"}");

        assertThat(adquirienteDeLaUnicaFactura())
                .as("perder el adquiriente por una comilla es el peor final posible")
                .containsEntry("nombre", "Ferretería El Roble");
    }

    @Test
    @DisplayName("Un pedido de restaurante, que nunca lleva cliente, también sale con el genérico")
    void elPedidoDeRestauranteTambien() {
        resolucionEnElNegocio();
        Map<String, Object> payload = new HashMap<>();
        payload.put("negocio_id", negocio.toString());
        payload.put("comanda_id", UUID.randomUUID().toString());
        payload.put("numero", "C-1");
        payload.put("emisor", Map.of("razon_social", "Mi Negocio SAS", "nit", "900111222"));
        payload.put("lineas", List.of(linea()));
        try {
            MessageProperties props = new MessageProperties();
            props.setMessageId(UUID.randomUUID().toString());
            props.setReceivedRoutingKey("pedido_completado");
            consumidor.recibir(MessageBuilder.withBody(json.writeValueAsBytes(payload))
                    .andProperties(props).build());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }

        assertThat(adquirienteDeLaUnicaFactura()).containsEntry("nombre", "Consumidor final");
    }
}
