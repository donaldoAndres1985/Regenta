package com.regenta.alertas.infra;

import java.nio.charset.StandardCharsets;
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
import com.regenta.alertas.aplicacion.VigilanciaDeInventario;
import com.regenta.comun.eventos.InboxIdempotente;
import com.regenta.comun.negocio.ContextoDeNegocio;
import com.regenta.comun.negocio.DatosDelNegocio;

/**
 * Escucha los hechos de inventario y los pasa por el motor de alertas (HU-093).
 * Pasa por el Inbox: un evento repetido no genera una segunda alerta ni cierra
 * dos veces.
 *
 * <ul>
 *   <li>{@code stock_bajo_minimo} → alerta de stock (criterio 1).</li>
 *   <li>{@code lote_por_vencer} → alerta de vencimiento (criterio 2).</li>
 *   <li>{@code stock_normalizado} → resuelve la alerta de stock (criterio 4).</li>
 * </ul>
 */
@Component
public class ConsumidorDeInventario {

    private final InboxIdempotente inbox;
    private final VigilanciaDeInventario vigilancia;
    private final ObjectMapper json;

    public ConsumidorDeInventario(InboxIdempotente inbox, VigilanciaDeInventario vigilancia,
            ObjectMapper json) {
        this.inbox = inbox;
        this.vigilancia = vigilancia;
        this.json = json;
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "alertas.hechos-de-inventario", durable = "true"),
            exchange = @Exchange(name = "${regenta.eventos.exchange:regenta.eventos}",
                    type = ExchangeTypes.TOPIC, durable = "true"),
            key = {"stock_bajo_minimo", "lote_por_vencer", "stock_normalizado"}))
    public void recibir(Message mensaje) {
        var props = mensaje.getMessageProperties();
        UUID mensajeId = UUID.fromString(props.getMessageId());
        String tipoEvento = props.getReceivedRoutingKey();
        String cuerpo = new String(mensaje.getBody(), StandardCharsets.UTF_8);
        Map<String, Object> datos = leer(cuerpo);

        Object negocio = datos.get("negocio_id");
        if (negocio == null) {
            return;
        }
        UUID negocioId = UUID.fromString(negocio.toString());

        ContextoDeNegocio.en(contextoDe(negocioId), () -> inbox.procesarUnaVez(
                mensajeId, negocioId, tipoEvento, cuerpo, payload -> despachar(tipoEvento, datos)));
    }

    private void despachar(String tipoEvento, Map<String, Object> datos) {
        switch (tipoEvento) {
            case "stock_bajo_minimo" -> vigilancia.alStockBajoMinimo(datos);
            case "lote_por_vencer" -> vigilancia.alLotePorVencer(datos);
            case "stock_normalizado" -> vigilancia.alStockNormalizado(datos);
            default -> { /* clave no esperada: se ignora */ }
        }
    }

    private Map<String, Object> leer(String cuerpo) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> mapa = json.readValue(cuerpo, Map.class);
            return mapa;
        } catch (Exception e) {
            throw new IllegalArgumentException("No se pudo leer el evento de inventario", e);
        }
    }

    private static DatosDelNegocio contextoDe(UUID negocioId) {
        return new DatosDelNegocio(negocioId, null, "", "VENTA_DIRECTA", Set.of(),
                Set.of("ALERTAS"), Set.of(), Set.of());
    }
}
