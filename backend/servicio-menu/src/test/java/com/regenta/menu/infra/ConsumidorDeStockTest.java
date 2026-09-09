package com.regenta.menu.infra;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
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

/** HU-080 criterio 3. Un insumo que queda en cero agota los ítems que lo usan. */
class ConsumidorDeStockTest extends BaseDeMenu {

    private static final Set<String> ADMIN = Set.of("MENU_PLATO_VER", "MENU_PLATO_CREAR",
            "MENU_PLATO_EDITAR");

    @Autowired
    private ConsumidorDeStock consumidor;
    @Autowired
    private GestionDeCartas cartas;
    @Autowired
    private GestionDeItemsDeMenu items;
    @Autowired
    private GestionDeRecetas recetas;
    @Autowired
    private GestionDeDisponibilidad disponibilidad;
    @Autowired
    private ObjectMapper json;

    private final UUID negocioA = UUID.randomUUID();
    private final UUID admin = UUID.randomUUID();
    private final UUID carne = UUID.randomUUID();

    private UUID itemQueUsa(UUID insumo) {
        UUID carta = enContexto(negocioA, admin, ADMIN, () -> cartas.crear(new SolicitudDeCarta(
                "Carta " + UUID.randomUUID(), null, null, null, null, null, null, false, null)))
                .id();
        UUID cat = enContexto(negocioA, admin, ADMIN, () -> cartas.crearCategoria(carta,
                new SolicitudDeCategoria("Fuertes", null, 0, null))).id();
        UUID it = enContexto(negocioA, admin, ADMIN, () -> items.crear(new SolicitudDeItem(cat, null,
                "IT-" + UUID.randomUUID().toString().substring(0, 8), "Bandeja", null, "PLATO",
                new BigDecimal("30000"), null, true, null, "FUERTE", null, null, 0))).id();
        enContexto(negocioA, admin, ADMIN, () -> recetas.agregarLinea(it,
                new SolicitudDeLineaDeReceta(insumo, "Carne", new BigDecimal("0.2"), "kg",
                        BigDecimal.ZERO, false)));
        return it;
    }

    private Message stock(String messageId, UUID productoId, String saldoPosterior) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("negocio_id", negocioA.toString());
        payload.put("producto_id", productoId.toString());
        payload.put("bodega_id", UUID.randomUUID().toString());
        payload.put("tipo", "SALIDA");
        payload.put("cantidad", new BigDecimal("0.2"));
        payload.put("saldo_posterior", new BigDecimal(saldoPosterior));
        try {
            MessageProperties props = new MessageProperties();
            props.setMessageId(messageId);
            props.setReceivedRoutingKey("stock_actualizado");
            return MessageBuilder.withBody(json.writeValueAsBytes(payload)).andProperties(props)
                    .build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private boolean agotado(UUID itemId) {
        return enContexto(negocioA, admin, ADMIN,
                () -> disponibilidad.delDia(itemId, LocalDate.now())).agotado();
    }

    private long itemAgotadoDe(UUID itemId) {
        return contar("SELECT count(*) FROM outbox_eventos WHERE agregado_id = '" + itemId
                + "' AND tipo_evento = 'item_agotado'");
    }

    @Test
    @DisplayName("Criterio 3: un stock_actualizado que deja el insumo en cero agota los ítems que lo requieren")
    void insumoEnCeroAgota() {
        UUID it = itemQueUsa(carne);

        consumidor.recibir(stock(UUID.randomUUID().toString(), carne, "0"));

        assertThat(agotado(it)).isTrue();
        assertThat(itemAgotadoDe(it)).isEqualTo(1);
    }

    @Test
    @DisplayName("Un stock_actualizado con saldo positivo no agota nada")
    void saldoPositivoNoAgota() {
        UUID it = itemQueUsa(carne);
        consumidor.recibir(stock(UUID.randomUUID().toString(), carne, "5"));
        assertThat(agotado(it)).isFalse();
    }

    @Test
    @DisplayName("Un insumo que no está en ninguna receta no agota nada y no falla")
    void insumoSinRecetas() {
        UUID it = itemQueUsa(carne);
        consumidor.recibir(stock(UUID.randomUUID().toString(), UUID.randomUUID(), "0"));
        assertThat(agotado(it)).isFalse();
    }

    @Test
    @DisplayName("El mismo evento dos veces agota una sola vez (Inbox)")
    void idempotente() {
        UUID it = itemQueUsa(carne);
        String id = UUID.randomUUID().toString();
        consumidor.recibir(stock(id, carne, "0"));
        consumidor.recibir(stock(id, carne, "0"));
        assertThat(itemAgotadoDe(it)).isEqualTo(1);
    }
}
