package com.regenta.reportes.infra;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
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
 * HU-096 criterios 1 y 5. Al llegar venta_completada se insertan las filas de
 * hecho con sus dimensiones resueltas; el mismo evento dos veces no duplica
 * nada (equivale a "si se reconstruye desde los eventos, queda idéntico").
 */
class ConsumidorDeVentasCompletadasTest extends BaseDeReportes {

    @Autowired
    private ConsumidorDeVentasCompletadas consumidor;
    @Autowired
    private ObjectMapper json;

    private final UUID negocioA = UUID.randomUUID();
    private final UUID negocioB = UUID.randomUUID();
    private final UUID producto = UUID.randomUUID();
    private final UUID cliente = UUID.randomUUID();
    private final UUID usuario = UUID.randomUUID();
    private final UUID bodega = UUID.randomUUID();

    private Message ventaCompletada(String messageId, UUID negocio, UUID ventaId) {
        Map<String, Object> linea = Map.of(
                "producto_id", producto.toString(),
                "sku", "SKU-1",
                "nombre", "Bandeja paisa",
                "cantidad", 2,
                "precio_unitario", "32000",
                "descuento_valor", "0",
                "impuesto_valor", "0",
                "total", "64000",
                "costo_unitario", "12000");
        Map<String, Object> payload = Map.of(
                "negocio_id", negocio.toString(),
                "venta_id", ventaId.toString(),
                "numero", "V-0001",
                "bodega_id", bodega.toString(),
                "cliente_id", cliente.toString(),
                "usuario_id", usuario.toString(),
                "canal", "MOSTRADOR",
                "fecha", OffsetDateTime.now().toString(),
                "total", "64000",
                "lineas", List.of(linea));
        return construir(messageId, "venta_completada", payload);
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
    @DisplayName("Criterio 1: inserta una fila de hecho por línea, con las dimensiones resueltas")
    void insertaHechoConDimensiones() {
        UUID ventaId = UUID.randomUUID();
        consumidor.recibir(ventaCompletada(UUID.randomUUID().toString(), negocioA, ventaId));

        assertThat(contar("select count(*) from hechos_venta where venta_id = '" + ventaId + "'"))
                .isEqualTo(1);
        assertThat(comoElServicio(negocioA, "select cantidad, monto_neto, costo from hechos_venta "
                + "where venta_id = '" + ventaId + "'")).isNotEmpty();
        assertThat(contar("select count(*) from dim_producto where producto_id = '" + producto + "'"))
                .isEqualTo(1);
        assertThat(contar("select count(*) from dim_cliente where cliente_id = '" + cliente + "'"))
                .isEqualTo(1);
        assertThat(contar("select count(*) from dim_usuario where usuario_id = '" + usuario + "'"))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("Criterio 5: el mismo evento entregado dos veces no duplica el hecho")
    void idempotente() {
        UUID ventaId = UUID.randomUUID();
        String mensajeId = UUID.randomUUID().toString();

        consumidor.recibir(ventaCompletada(mensajeId, negocioA, ventaId));
        consumidor.recibir(ventaCompletada(mensajeId, negocioA, ventaId));

        assertThat(contar("select count(*) from hechos_venta where venta_id = '" + ventaId + "'"))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("HU-097 criterio 1: también actualiza el agregado diario de la venta")
    void actualizaElAgregadoDiario() {
        UUID ventaId = UUID.randomUUID();
        consumidor.recibir(ventaCompletada(UUID.randomUUID().toString(), negocioA, ventaId));

        assertThat(consultar("select num_documentos::text from agregados_diarios "
                + "where negocio_id = '" + negocioA + "' and patron = 'VENTA_DIRECTA'"))
                .containsExactly("1");
        assertThat(consultar("select monto_neto::text from agregados_diarios "
                + "where negocio_id = '" + negocioA + "' and patron = 'VENTA_DIRECTA'"))
                .containsExactly("64000.0000");
    }

    @Test
    @DisplayName("El segundo negocio no ve los hechos ni las dimensiones del primero")
    void aislamiento() {
        UUID ventaId = UUID.randomUUID();
        consumidor.recibir(ventaCompletada(UUID.randomUUID().toString(), negocioA, ventaId));

        assertThat(comoElServicio(negocioB, "select count(*) from hechos_venta where venta_id = '"
                + ventaId + "'")).containsExactly("0");
    }
}
