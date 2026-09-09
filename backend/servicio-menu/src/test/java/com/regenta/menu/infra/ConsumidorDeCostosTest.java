package com.regenta.menu.infra;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
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
import com.regenta.menu.BaseDeMenu;
import com.regenta.menu.aplicacion.GestionDeCartas;
import com.regenta.menu.aplicacion.GestionDeItemsDeMenu;
import com.regenta.menu.aplicacion.GestionDeRecetas;
import com.regenta.menu.aplicacion.SolicitudDeCarta;
import com.regenta.menu.aplicacion.SolicitudDeCategoria;
import com.regenta.menu.aplicacion.SolicitudDeItem;
import com.regenta.menu.aplicacion.SolicitudDeLineaDeReceta;

/** HU-079 criterio 2. Un cambio de costo de insumo recalcula el costo estimado de los ítems. */
class ConsumidorDeCostosTest extends BaseDeMenu {

    private static final Set<String> ADMIN = Set.of("MENU_PLATO_VER", "MENU_PLATO_CREAR",
            "MENU_PLATO_EDITAR");

    @Autowired
    private ConsumidorDeCostos consumidor;
    @Autowired
    private GestionDeRecetas recetas;
    @Autowired
    private GestionDeCartas cartas;
    @Autowired
    private GestionDeItemsDeMenu items;
    @Autowired
    private CostosDeInventarioStub costos;
    @Autowired
    private ObjectMapper json;

    private final UUID negocioA = UUID.randomUUID();
    private final UUID admin = UUID.randomUUID();
    private final UUID carne = UUID.randomUUID();

    @BeforeEach
    void limpiar() {
        costos.reiniciar();
    }

    @Test
    @DisplayName("Al recibir costo_producto_actualizado, el ítem recalcula su costo estimado")
    void recalculaAlCambiarElCosto() {
        UUID carta = enContexto(negocioA, admin, ADMIN, () -> cartas.crear(new SolicitudDeCarta(
                "Carta", null, null, null, null, null, null, false, null))).id();
        UUID cat = enContexto(negocioA, admin, ADMIN, () -> cartas.crearCategoria(carta,
                new SolicitudDeCategoria("Fuertes", null, 0, null))).id();
        UUID it = enContexto(negocioA, admin, ADMIN, () -> items.crear(new SolicitudDeItem(cat, null,
                "BANDEJA", "Bandeja", null, "PLATO", new BigDecimal("30000"), null, true, null,
                "FUERTE", null, null, 0))).id();

        costos.cargarCosto(carne, new BigDecimal("40000"));
        enContexto(negocioA, admin, ADMIN, () -> recetas.agregarLinea(it,
                new SolicitudDeLineaDeReceta(carne, "Carne", new BigDecimal("0.5"), "kg",
                        BigDecimal.ZERO, false)));
        assertThat(enContexto(negocioA, admin, ADMIN, () -> recetas.ver(it)).costoEstimado())
                .isEqualByComparingTo("20000");

        costos.cargarCosto(carne, new BigDecimal("60000"));
        consumidor.recibir(evento(UUID.randomUUID().toString(), carne));

        assertThat(enContexto(negocioA, admin, ADMIN, () -> recetas.ver(it)).costoEstimado())
                .isEqualByComparingTo("30000");
    }

    private Message evento(String messageId, UUID productoId) {
        Map<String, Object> payload = Map.of("negocio_id", negocioA.toString(),
                "producto_id", productoId.toString());
        try {
            MessageProperties props = new MessageProperties();
            props.setMessageId(messageId);
            props.setReceivedRoutingKey("costo_producto_actualizado");
            return MessageBuilder.withBody(json.writeValueAsBytes(payload)).andProperties(props)
                    .build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
