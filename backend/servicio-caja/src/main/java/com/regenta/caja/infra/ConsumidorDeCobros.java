package com.regenta.caja.infra;

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
import com.regenta.caja.aplicacion.RegistroDeMovimientos;
import com.regenta.comun.eventos.InboxIdempotente;
import com.regenta.comun.negocio.ContextoDeNegocio;
import com.regenta.comun.negocio.DatosDelNegocio;

/**
 * Todo cobro entra a la caja sin importar de qué patrón viene (HU-060): venta
 * directa, comanda o reserva. La clave de enrutamiento decide el
 * {@code origen_tipo}. Pasa por el Inbox: un evento repetido no registra dos
 * veces (criterio 4).
 */
@Component
public class ConsumidorDeCobros {

    private final InboxIdempotente inbox;
    private final RegistroDeMovimientos movimientos;
    private final ObjectMapper json;

    public ConsumidorDeCobros(InboxIdempotente inbox, RegistroDeMovimientos movimientos,
            ObjectMapper json) {
        this.inbox = inbox;
        this.movimientos = movimientos;
        this.json = json;
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "caja.cobros", durable = "true"),
            exchange = @Exchange(name = "${regenta.eventos.exchange:regenta.eventos}",
                    type = ExchangeTypes.TOPIC, durable = "true"),
            key = {"venta_completada", "pedido_completado", "anticipo_reserva_cobrado"}))
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
        String origenTipo = origenDe(tipoEvento);

        ContextoDeNegocio.en(contextoDe(negocioId), () -> inbox.procesarUnaVez(
                mensajeId, negocioId, tipoEvento, cuerpo,
                payload -> movimientos.registrarDesdeCobro(origenTipo, datos)));
    }

    private static String origenDe(String tipoEvento) {
        return switch (tipoEvento) {
            case "pedido_completado" -> "COMANDA";
            case "anticipo_reserva_cobrado" -> "RESERVA";
            default -> "VENTA";
        };
    }

    private Map<String, Object> leer(String cuerpo) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> mapa = json.readValue(cuerpo, Map.class);
            return mapa;
        } catch (Exception e) {
            throw new IllegalArgumentException("No se pudo leer el evento de cobro", e);
        }
    }

    private static DatosDelNegocio contextoDe(UUID negocioId) {
        return new DatosDelNegocio(negocioId, null, "", "VENTA_DIRECTA", Set.of(),
                Set.of("CAJA"), Set.of(), Set.of());
    }
}
