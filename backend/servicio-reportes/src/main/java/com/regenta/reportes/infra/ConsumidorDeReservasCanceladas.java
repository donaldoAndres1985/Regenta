package com.regenta.reportes.infra;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
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
import com.regenta.reportes.aplicacion.ResolverDeDimensiones;

/**
 * Puebla {@code hechos_reserva} con las reservas que no llegaron a estancia
 * (HU-135). {@code penalizacion} se quedaba siempre en cero porque nadie
 * escuchaba {@code reserva_cancelada} ni {@code reserva_no_show}, aunque los
 * dos ya publican la penalización real (HU-071).
 *
 * <p>A propósito no llama a {@code AgregadorDeOcupacion}: una reserva
 * cancelada o un no-show nunca llegan a {@code estancia_finalizada}, así que
 * esas noches nunca entraron a {@code ocupacion_diaria} (criterio 4).
 */
@Component
public class ConsumidorDeReservasCanceladas {

    private final InboxIdempotente inbox;
    private final ResolverDeDimensiones dimensiones;
    private final EscritorDeHechos hechos;
    private final ObjectMapper json;

    public ConsumidorDeReservasCanceladas(InboxIdempotente inbox, ResolverDeDimensiones dimensiones,
            EscritorDeHechos hechos, ObjectMapper json) {
        this.inbox = inbox;
        this.dimensiones = dimensiones;
        this.hechos = hechos;
        this.json = json;
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "reportes.reservas-canceladas", durable = "true"),
            exchange = @Exchange(name = "${regenta.eventos.exchange:regenta.eventos}",
                    type = ExchangeTypes.TOPIC, durable = "true"),
            key = {"reserva_cancelada", "reserva_no_show"}))
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
        String estadoFinal = "reserva_no_show".equals(tipoEvento) ? "NO_SHOW" : "CANCELADA";

        ContextoDeNegocio.en(contextoDe(negocioId), () -> inbox.procesarUnaVez(mensajeId, negocioId,
                tipoEvento, cuerpo, payload -> procesar(negocioId, estadoFinal, datos)));
    }

    private void procesar(UUID negocioId, String estadoFinal, Map<String, Object> datos) {
        UUID reservaId = uuid(datos.get("reserva_id"));
        UUID clienteId = uuid(datos.get("cliente_id"));
        UUID recursoId = uuid(datos.get("recurso_id"));
        UUID tipoRecursoId = uuid(datos.get("tipo_recurso_id"));
        int noches = datos.get("noches") == null ? 0 : Integer.parseInt(datos.get("noches").toString());
        BigDecimal penalizacion = numero(datos.get("penalizacion"));
        OffsetDateTime ocurridoEn = OffsetDateTime.now();

        int fechaId = dimensiones.fechaId(ocurridoEn);
        Long clienteSk = dimensiones.clienteSk(negocioId, clienteId, null);

        // Sin ocupación que registrar (criterio 4) y sin ingreso reconocido: la
        // habitación no se vendió. Lo único que hubo es la penalización.
        hechos.insertarReserva(negocioId, fechaId, ocurridoEn, null, clienteSk, reservaId, tipoRecursoId,
                recursoId, estadoFinal, noches, BigDecimal.ZERO, BigDecimal.ZERO, penalizacion, null);
    }

    private static BigDecimal numero(Object valor) {
        return valor == null ? BigDecimal.ZERO : new BigDecimal(valor.toString());
    }

    private static UUID uuid(Object valor) {
        return valor == null ? null : UUID.fromString(valor.toString());
    }

    private Map<String, Object> leer(String cuerpo) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> mapa = json.readValue(cuerpo, Map.class);
            return mapa;
        } catch (Exception e) {
            throw new IllegalArgumentException("No se pudo leer el evento de cancelación", e);
        }
    }

    private static DatosDelNegocio contextoDe(UUID negocioId) {
        return new DatosDelNegocio(negocioId, null, "", "RESERVA", Set.of(), Set.of("REPORTES"), Set.of(),
                Set.of());
    }
}
