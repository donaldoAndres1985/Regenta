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
    private ConsumidorDeMesas consumidorDeMesas;
    @Autowired
    private ObjectMapper json;

    private final UUID negocioA = UUID.randomUUID();
    private final UUID negocioB = UUID.randomUUID();
    private final UUID mesero = UUID.randomUUID();
    private final UUID item = UUID.randomUUID();

    private Message pedidoCompletado(UUID negocio, UUID comandaId) {
        return pedidoCompletado(negocio, comandaId, null);
    }

    private Message pedidoCompletado(UUID negocio, UUID comandaId, UUID mesaId) {
        Map<String, Object> linea = Map.of("item_menu_id", item.toString(), "nombre", "Churrasco",
                "cantidad", 1, "monto_neto", "38000", "costo", "15000", "tiempo_preparacion_min", 12);
        java.util.Map<String, Object> payload = new java.util.HashMap<>(Map.of("negocio_id",
                negocio.toString(), "comanda_id", comandaId.toString(), "usuario_id", mesero.toString(),
                "propina", "3800", "tiempo_mesa_min", 45, "lineas", List.of(linea)));
        if (mesaId != null) {
            payload.put("mesa_id", mesaId.toString());
        }
        try {
            MessageProperties props = new MessageProperties();
            props.setMessageId(UUID.randomUUID().toString());
            props.setReceivedRoutingKey("pedido_completado");
            return MessageBuilder.withBody(json.writeValueAsBytes(payload)).andProperties(props).build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Message mesaEvento(String tipoEvento, UUID negocio, UUID mesaId, String codigo,
            boolean activa) {
        Map<String, Object> payload = new java.util.LinkedHashMap<>();
        payload.put("negocio_id", negocio.toString());
        payload.put("mesa_id", mesaId.toString());
        payload.put("zona_id", null);
        payload.put("zona_nombre", null);
        payload.put("codigo", codigo);
        payload.put("nombre", "Mesa " + codigo);
        payload.put("capacidad", 4);
        payload.put("activa", activa);
        try {
            MessageProperties props = new MessageProperties();
            props.setMessageId(UUID.randomUUID().toString());
            props.setReceivedRoutingKey(tipoEvento);
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

    @Test
    @DisplayName("HU-097 criterio 1: también actualiza el agregado diario del pedido")
    void actualizaElAgregadoDiario() {
        UUID comandaId = UUID.randomUUID();
        consumidor.recibir(pedidoCompletado(negocioA, comandaId));

        assertThat(consultar("select num_documentos::text from agregados_diarios "
                + "where negocio_id = '" + negocioA + "' and patron = 'COMANDA'"))
                .containsExactly("1");
        assertThat(consultar("select monto_neto::text from agregados_diarios "
                + "where negocio_id = '" + negocioA + "' and patron = 'COMANDA'"))
                .containsExactly("38000.0000");
    }

    @Test
    @DisplayName("HU-134 criterio 3: el hecho queda con el código legible de la mesa")
    void guardaElCodigoDeLaMesa() {
        UUID mesaId = UUID.randomUUID();
        consumidorDeMesas.recibir(mesaEvento("mesa_creada", negocioA, mesaId, "T7", true));
        UUID comandaId = UUID.randomUUID();

        consumidor.recibir(pedidoCompletado(negocioA, comandaId, mesaId));

        assertThat(comoElServicio(negocioA, "select mesa_codigo from hechos_comanda "
                + "where comanda_id = '" + comandaId + "'")).containsExactly("T7");
    }

    @Test
    @DisplayName("Sin mesa en el catálogo, el hecho queda con mesa_codigo en null, no falla")
    void sinMesaEnElCatalogoNoFalla() {
        UUID comandaId = UUID.randomUUID();

        consumidor.recibir(pedidoCompletado(negocioA, comandaId, UUID.randomUUID()));

        assertThat(comoElServicio(negocioA, "select mesa_codigo from hechos_comanda "
                + "where comanda_id = '" + comandaId + "'")).containsExactly((String) null);
    }

    @Test
    @DisplayName("HU-134 criterio 5: una mesa eliminada después no cambia el código ya guardado")
    void mesaEliminadaDespuesNoCambiaElHechoYaGuardado() {
        UUID mesaId = UUID.randomUUID();
        consumidorDeMesas.recibir(mesaEvento("mesa_creada", negocioA, mesaId, "T8", true));
        UUID comandaId = UUID.randomUUID();
        consumidor.recibir(pedidoCompletado(negocioA, comandaId, mesaId));

        consumidorDeMesas.recibir(mesaEvento("mesa_eliminada", negocioA, mesaId, "T8", false));

        assertThat(comoElServicio(negocioA, "select mesa_codigo from hechos_comanda "
                + "where comanda_id = '" + comandaId + "'")).containsExactly("T8");
    }
}
