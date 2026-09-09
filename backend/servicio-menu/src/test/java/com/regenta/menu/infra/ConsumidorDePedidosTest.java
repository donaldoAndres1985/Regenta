package com.regenta.menu.infra;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
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
import com.regenta.menu.BaseDeMenu;
import com.regenta.menu.aplicacion.GestionDeCartas;
import com.regenta.menu.aplicacion.GestionDeDisponibilidad;
import com.regenta.menu.aplicacion.GestionDeItemsDeMenu;
import com.regenta.menu.aplicacion.GestionDeRecetas;
import com.regenta.menu.aplicacion.SolicitudDeCarta;
import com.regenta.menu.aplicacion.SolicitudDeCategoria;
import com.regenta.menu.aplicacion.SolicitudDeItem;
import com.regenta.menu.aplicacion.SolicitudDeLineaDeReceta;

/** HU-079 criterio 3. Al cerrarse una comanda se publica insumos_consumidos. */
class ConsumidorDePedidosTest extends BaseDeMenu {

    private static final Set<String> ADMIN = Set.of("MENU_PLATO_VER", "MENU_PLATO_CREAR",
            "MENU_PLATO_EDITAR");

    @Autowired
    private ConsumidorDePedidos consumidor;
    @Autowired
    private GestionDeRecetas recetas;
    @Autowired
    private GestionDeCartas cartas;
    @Autowired
    private GestionDeItemsDeMenu items;
    @Autowired
    private GestionDeDisponibilidad disponibilidad;
    @Autowired
    private ObjectMapper json;

    private final UUID negocioA = UUID.randomUUID();
    private final UUID admin = UUID.randomUUID();
    private final UUID carne = UUID.randomUUID();

    private UUID itemConReceta(boolean conReceta) {
        UUID carta = enContexto(negocioA, admin, ADMIN, () -> cartas.crear(new SolicitudDeCarta(
                "Carta " + UUID.randomUUID(), null, null, null, null, null, null, false, null)))
                .id();
        UUID cat = enContexto(negocioA, admin, ADMIN, () -> cartas.crearCategoria(carta,
                new SolicitudDeCategoria("Fuertes", null, 0, null))).id();
        UUID it = enContexto(negocioA, admin, ADMIN, () -> items.crear(new SolicitudDeItem(cat, null,
                "IT-" + UUID.randomUUID().toString().substring(0, 8), "Bandeja", null, "PLATO",
                new BigDecimal("30000"), null, true, null, "FUERTE", null, null, 0))).id();
        if (conReceta) {
            enContexto(negocioA, admin, ADMIN, () -> recetas.agregarLinea(it,
                    new SolicitudDeLineaDeReceta(carne, "Carne", new BigDecimal("0.2"), "kg",
                            BigDecimal.ZERO, false)));
        }
        return it;
    }

    private Message pedido(String messageId, UUID comandaId, UUID itemId, int cantidad) {
        Map<String, Object> payload = Map.of(
                "negocio_id", negocioA.toString(),
                "comanda_id", comandaId.toString(),
                "lineas", List.of(Map.of("item_menu_id", itemId.toString(), "cantidad", cantidad)));
        try {
            MessageProperties props = new MessageProperties();
            props.setMessageId(messageId);
            props.setReceivedRoutingKey("pedido_completado");
            return MessageBuilder.withBody(json.writeValueAsBytes(payload)).andProperties(props)
                    .build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private long insumosDe(UUID comandaId) {
        return contar("SELECT count(*) FROM outbox_eventos WHERE agregado_id = '" + comandaId
                + "' AND tipo_evento = 'insumos_consumidos'");
    }

    @Test
    @DisplayName("Una comanda con recetas publica insumos_consumidos con productos y cantidades")
    void publicaInsumos() {
        UUID it = itemConReceta(true);
        UUID comanda = UUID.randomUUID();

        consumidor.recibir(pedido(UUID.randomUUID().toString(), comanda, it, 3));

        assertThat(insumosDe(comanda)).isEqualTo(1);
        String payload = consultar("SELECT payload FROM outbox_eventos WHERE agregado_id = '"
                + comanda + "' AND tipo_evento = 'insumos_consumidos'").get(0);
        assertThat(payload).contains("\"producto_id\": \"" + carne + "\"");
        assertThat(payload).contains("0.6");
    }

    @Test
    @DisplayName("La misma comanda dos veces no descuenta dos veces (Inbox)")
    void idempotente() {
        UUID it = itemConReceta(true);
        UUID comanda = UUID.randomUUID();
        String id = UUID.randomUUID().toString();

        consumidor.recibir(pedido(id, comanda, it, 1));
        consumidor.recibir(pedido(id, comanda, it, 1));

        assertThat(insumosDe(comanda)).isEqualTo(1);
    }

    @Test
    @DisplayName("Criterio 5: una comanda de un ítem sin receta no publica nada")
    void sinReceta() {
        UUID it = itemConReceta(false);
        UUID comanda = UUID.randomUUID();

        consumidor.recibir(pedido(UUID.randomUUID().toString(), comanda, it, 2));

        assertThat(insumosDe(comanda)).isZero();
    }

    @Test
    @DisplayName("HU-080: cerrar una comanda descuenta el cupo diario del ítem y lo agota solo")
    void descuentaCupoDiario() {
        UUID it = itemConReceta(false);
        enContexto(negocioA, admin, ADMIN, () -> disponibilidad.fijarCupo(it, 3));

        consumidor.recibir(pedido(UUID.randomUUID().toString(), UUID.randomUUID(), it, 3));

        assertThat(enContexto(negocioA, admin, ADMIN,
                () -> disponibilidad.delDia(it, java.time.LocalDate.now())).agotado()).isTrue();
    }
}
