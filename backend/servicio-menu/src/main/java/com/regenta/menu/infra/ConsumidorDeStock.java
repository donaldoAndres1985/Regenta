package com.regenta.menu.infra;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.regenta.comun.eventos.InboxIdempotente;
import com.regenta.comun.negocio.ContextoDeNegocio;
import com.regenta.comun.negocio.DatosDelNegocio;
import com.regenta.menu.aplicacion.GestionDeDisponibilidad;

/**
 * Cuando un movimiento de inventario deja un insumo en cero, marca agotados los
 * ítems cuya receta lo usa (HU-080 criterio 3). Pasa por el Inbox: el mismo
 * evento repetido no vuelve a publicar {@code item_agotado}.
 *
 * <p>Payload esperado de {@code stock_actualizado}:
 * {@code {negocio_id, producto_id, bodega_id, tipo, cantidad, saldo_posterior}}.
 */
@Component
public class ConsumidorDeStock {

    private final InboxIdempotente inbox;
    private final GestionDeDisponibilidad disponibilidad;
    private final ObjectMapper json;

    public ConsumidorDeStock(InboxIdempotente inbox, GestionDeDisponibilidad disponibilidad,
            ObjectMapper json) {
        this.inbox = inbox;
        this.disponibilidad = disponibilidad;
        this.json = json;
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "menu.stock-agotado", durable = "true"),
            exchange = @Exchange(name = "${regenta.eventos.exchange:regenta.eventos}",
                    type = ExchangeTypes.TOPIC, durable = "true"),
            key = {"stock_actualizado"}))
    public void recibir(Message mensaje) {
        var props = mensaje.getMessageProperties();
        UUID mensajeId = UUID.fromString(props.getMessageId());
        String tipoEvento = props.getReceivedRoutingKey();
        String cuerpo = new String(mensaje.getBody(), StandardCharsets.UTF_8);
        Map<String, Object> datos = leer(cuerpo);

        Object negocio = datos.get("negocio_id");
        Object producto = datos.get("producto_id");
        if (negocio == null || producto == null || !enCero(datos.get("saldo_posterior"))) {
            return;
        }
        UUID negocioId = UUID.fromString(negocio.toString());
        UUID productoId = UUID.fromString(producto.toString());

        ContextoDeNegocio.en(contextoDe(negocioId), () -> inbox.procesarUnaVez(
                mensajeId, negocioId, tipoEvento, cuerpo,
                payload -> disponibilidad.agotarPorInsumoSinStock(negocioId, productoId,
                        LocalDate.now())));
    }

    private static boolean enCero(Object saldo) {
        if (saldo == null) {
            return false;
        }
        try {
            return new BigDecimal(saldo.toString()).signum() <= 0;
        } catch (NumberFormatException noEsNumero) {
            return false;
        }
    }

    private Map<String, Object> leer(String cuerpo) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> mapa = json.readValue(cuerpo, Map.class);
            return mapa;
        } catch (Exception e) {
            throw new IllegalArgumentException("No se pudo leer el evento stock_actualizado", e);
        }
    }

    private static DatosDelNegocio contextoDe(UUID negocioId) {
        return new DatosDelNegocio(negocioId, null, "", "COMANDA", Set.of(), Set.of("MENU"),
                Set.of(), Set.of());
    }
}
