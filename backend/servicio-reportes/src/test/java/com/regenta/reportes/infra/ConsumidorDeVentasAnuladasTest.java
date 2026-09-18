package com.regenta.reportes.infra;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.regenta.reportes.BaseDeReportes;

/**
 * HU-097 criterio 4: anular una venta ajusta el agregado diario a la baja. No
 * toca {@code hechos_venta}: ese histórico queda como pasó, la venta incluida.
 */
class ConsumidorDeVentasAnuladasTest extends BaseDeReportes {

    @Autowired
    private ConsumidorDeVentasCompletadas consumidorDeCompletadas;
    @Autowired
    private ConsumidorDeVentasAnuladas consumidor;
    @Autowired
    private ObjectMapper json;

    private final UUID negocioA = UUID.randomUUID();
    private final UUID negocioB = UUID.randomUUID();
    private final UUID producto = UUID.randomUUID();
    private final UUID bodega = UUID.randomUUID();

    private Message ventaCompletada(UUID negocio, UUID ventaId) {
        Map<String, Object> linea = Map.of(
                "producto_id", producto.toString(), "sku", "SKU-1", "nombre", "Bandeja paisa",
                "cantidad", 2, "precio_unitario", "32000", "descuento_valor", "0", "impuesto_valor", "0",
                "total", "64000", "costo_unitario", "12000");
        Map<String, Object> payload = Map.of(
                "negocio_id", negocio.toString(), "venta_id", ventaId.toString(), "numero", "V-0001",
                "bodega_id", bodega.toString(), "canal", "MOSTRADOR", "fecha", OffsetDateTime.now().toString(),
                "total", "64000", "lineas", List.of(linea));
        return construir(UUID.randomUUID().toString(), "venta_completada", payload);
    }

    private Message ventaAnulada(String messageId, UUID negocio, UUID ventaId) {
        Map<String, Object> payload = Map.of(
                "negocio_id", negocio.toString(), "venta_id", ventaId.toString(), "numero", "V-0001",
                "motivo", "Error de digitación", "lineas", List.of());
        return construir(messageId, "venta_anulada", payload);
    }

    private Message construir(String messageId, String tipo, Map<String, Object> payload) {
        try {
            MessageProperties props = new MessageProperties();
            props.setMessageId(messageId);
            props.setReceivedRoutingKey(tipo);
            return MessageBuilder.withBody(json.writeValueAsBytes(payload)).andProperties(props).build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("Criterio 4: anular la venta deja el agregado del día en cero")
    void anularAjustaElAgregado() {
        UUID ventaId = UUID.randomUUID();
        consumidorDeCompletadas.recibir(ventaCompletada(negocioA, ventaId));
        consumidor.recibir(ventaAnulada(UUID.randomUUID().toString(), negocioA, ventaId));

        assertThat(consultar("select num_documentos::text from agregados_diarios "
                + "where negocio_id = '" + negocioA + "' and patron = 'VENTA_DIRECTA'"))
                .containsExactly("0");
        assertThat(consultar("select monto_neto::text from agregados_diarios "
                + "where negocio_id = '" + negocioA + "' and patron = 'VENTA_DIRECTA'"))
                .containsExactly("0.0000");
        // hechos_venta es el histórico: sigue teniendo la fila de la venta anulada.
        assertThat(contar("select count(*) from hechos_venta where venta_id = '" + ventaId + "'"))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("El mismo evento de anulación entregado dos veces no resta doble")
    void idempotente() {
        UUID ventaId = UUID.randomUUID();
        String mensajeId = UUID.randomUUID().toString();
        consumidorDeCompletadas.recibir(ventaCompletada(negocioA, ventaId));
        consumidor.recibir(ventaAnulada(mensajeId, negocioA, ventaId));
        consumidor.recibir(ventaAnulada(mensajeId, negocioA, ventaId));

        assertThat(consultar("select num_documentos::text from agregados_diarios "
                + "where negocio_id = '" + negocioA + "' and patron = 'VENTA_DIRECTA'"))
                .containsExactly("0");
    }

    @Test
    @DisplayName("El segundo negocio no se ve afectado por la anulación del primero")
    void aislamiento() {
        UUID ventaId = UUID.randomUUID();
        consumidorDeCompletadas.recibir(ventaCompletada(negocioA, ventaId));

        assertThat(comoElServicio(negocioB, "select count(*) from agregados_diarios where negocio_id = '"
                + negocioA + "'")).containsExactly("0");
    }
}
