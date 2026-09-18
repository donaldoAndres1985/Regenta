package com.regenta.reportes.infra;

import static org.assertj.core.api.Assertions.assertThat;

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
 * HU-098 criterio 3 (soporte). Puebla {@code hechos_inventario}, la tabla de la
 * que sale el reporte de rotación: sin movimientos registrados no hay de dónde
 * calcular los días sin movimiento de un producto.
 */
class ConsumidorDeStockActualizadoTest extends BaseDeReportes {

    @Autowired
    private ConsumidorDeStockActualizado consumidor;
    @Autowired
    private ObjectMapper json;

    private final UUID negocioA = UUID.randomUUID();
    private final UUID negocioB = UUID.randomUUID();
    private final UUID producto = UUID.randomUUID();
    private final UUID bodega = UUID.randomUUID();

    private Message stockActualizado(String messageId, UUID negocio) {
        Map<String, Object> payload = Map.of(
                "negocio_id", negocio.toString(), "producto_id", producto.toString(), "bodega_id",
                bodega.toString(), "tipo", "ENTRADA", "cantidad", "10", "saldo_posterior", "10");
        try {
            MessageProperties props = new MessageProperties();
            props.setMessageId(messageId);
            props.setReceivedRoutingKey("stock_actualizado");
            return MessageBuilder.withBody(json.writeValueAsBytes(payload)).andProperties(props).build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("stock_actualizado inserta una fila de hecho de inventario")
    void insertaHechoDeInventario() {
        consumidor.recibir(stockActualizado(UUID.randomUUID().toString(), negocioA));

        assertThat(comoElServicio(negocioA, "select tipo_movimiento, saldo_posterior "
                + "from hechos_inventario where negocio_id = '" + negocioA + "'")).isNotEmpty();
        assertThat(contar("select count(*) from hechos_inventario where negocio_id = '" + negocioA + "'"))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("El mismo evento entregado dos veces no duplica el hecho")
    void idempotente() {
        String mensajeId = UUID.randomUUID().toString();
        consumidor.recibir(stockActualizado(mensajeId, negocioA));
        consumidor.recibir(stockActualizado(mensajeId, negocioA));

        assertThat(contar("select count(*) from hechos_inventario where negocio_id = '" + negocioA + "'"))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("El segundo negocio no ve el hecho del primero")
    void aislamiento() {
        consumidor.recibir(stockActualizado(UUID.randomUUID().toString(), negocioA));

        assertThat(comoElServicio(negocioB,
                "select count(*) from hechos_inventario where negocio_id = '" + negocioA + "'"))
                .containsExactly("0");
    }
}
