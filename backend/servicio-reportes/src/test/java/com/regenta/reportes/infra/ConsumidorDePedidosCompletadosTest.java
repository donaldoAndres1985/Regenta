package com.regenta.reportes.infra;

import static org.assertj.core.api.Assertions.assertThat;

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

/** HU-096 criterio 4 (patrón Comanda). pedido_completado alimenta hechos_comanda. */
class ConsumidorDePedidosCompletadosTest extends BaseDeReportes {

    @Autowired
    private ConsumidorDePedidosCompletados consumidor;
    @Autowired
    private ObjectMapper json;

    private final UUID negocioA = UUID.randomUUID();
    private final UUID negocioB = UUID.randomUUID();
    private final UUID mesero = UUID.randomUUID();
    private final UUID item = UUID.randomUUID();

    private Message pedidoCompletado(UUID negocio, UUID comandaId) {
        Map<String, Object> linea = Map.of("item_menu_id", item.toString(), "nombre", "Churrasco",
                "cantidad", 1, "monto_neto", "38000", "costo", "15000", "tiempo_preparacion_min", 12);
        Map<String, Object> payload = Map.of("negocio_id", negocio.toString(), "comanda_id",
                comandaId.toString(), "usuario_id", mesero.toString(), "propina", "3800",
                "tiempo_mesa_min", 45, "lineas", List.of(linea));
        try {
            MessageProperties props = new MessageProperties();
            props.setMessageId(UUID.randomUUID().toString());
            props.setReceivedRoutingKey("pedido_completado");
            return MessageBuilder.withBody(json.writeValueAsBytes(payload)).andProperties(props).build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("Criterio 4: pedido_completado inserta una fila de hecho por línea, con el mesero resuelto")
    void insertaHechoDeComanda() {
        UUID comandaId = UUID.randomUUID();
        consumidor.recibir(pedidoCompletado(negocioA, comandaId));

        assertThat(contar("select count(*) from hechos_comanda where comanda_id = '" + comandaId + "'"))
                .isEqualTo(1);
        assertThat(comoElServicio(negocioA, "select item_nombre, propina, tiempo_mesa_min "
                + "from hechos_comanda where comanda_id = '" + comandaId + "'")).isNotEmpty();
        assertThat(contar("select count(*) from dim_usuario where usuario_id = '" + mesero + "'"))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("El segundo negocio no ve el hecho del primero")
    void aislamiento() {
        UUID comandaId = UUID.randomUUID();
        consumidor.recibir(pedidoCompletado(negocioA, comandaId));
        assertThat(comoElServicio(negocioB, "select count(*) from hechos_comanda where comanda_id = '"
                + comandaId + "'")).containsExactly("0");
    }
}
