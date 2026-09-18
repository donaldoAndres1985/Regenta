package com.regenta.reportes.infra;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
import com.regenta.reportes.aplicacion.AgregadorDeOcupacion;
import com.regenta.reportes.aplicacion.AgregadorDiario;
import com.regenta.reportes.aplicacion.FechaLocalDelNegocio;
import com.regenta.reportes.aplicacion.ResolverDeDimensiones;

/**
 * Puebla {@code hechos_reserva}, una fila por estancia cerrada (HU-096
 * criterio 4, patrón Reserva). Pasa por el Inbox.
 *
 * <p>Payload esperado de {@code estancia_finalizada} (HU-054): {@code
 * {negocio_id, reserva_id, cliente_id, recurso_id, tipo_recurso_id, noches,
 * total, consumos, check_out_en}}.
 */
@Component
public class ConsumidorDeEstanciasFinalizadas {

    private final InboxIdempotente inbox;
    private final ResolverDeDimensiones dimensiones;
    private final EscritorDeHechos hechos;
    private final AgregadorDiario agregador;
    private final AgregadorDeOcupacion ocupacion;
    private final FechaLocalDelNegocio fechaLocal;
    private final ObjectMapper json;

    public ConsumidorDeEstanciasFinalizadas(InboxIdempotente inbox, ResolverDeDimensiones dimensiones,
            EscritorDeHechos hechos, AgregadorDiario agregador, AgregadorDeOcupacion ocupacion,
            FechaLocalDelNegocio fechaLocal, ObjectMapper json) {
        this.inbox = inbox;
        this.dimensiones = dimensiones;
        this.hechos = hechos;
        this.agregador = agregador;
        this.ocupacion = ocupacion;
        this.fechaLocal = fechaLocal;
        this.json = json;
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "reportes.estancias-finalizadas", durable = "true"),
            exchange = @Exchange(name = "${regenta.eventos.exchange:regenta.eventos}",
                    type = ExchangeTypes.TOPIC, durable = "true"),
            key = {"estancia_finalizada"}))
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

        ContextoDeNegocio.en(contextoDe(negocioId), () -> inbox.procesarUnaVez(mensajeId, negocioId,
                tipoEvento, cuerpo, payload -> procesar(negocioId, datos)));
    }

    private void procesar(UUID negocioId, Map<String, Object> datos) {
        UUID reservaId = uuid(datos.get("reserva_id"));
        UUID clienteId = uuid(datos.get("cliente_id"));
        UUID recursoId = uuid(datos.get("recurso_id"));
        UUID tipoRecursoId = uuid(datos.get("tipo_recurso_id"));
        int noches = datos.get("noches") == null ? 1 : Integer.parseInt(datos.get("noches").toString());
        BigDecimal total = numero(datos.get("total"));
        BigDecimal consumos = numero(datos.get("consumos"));
        OffsetDateTime ocurridoEn = datos.get("check_out_en") == null ? OffsetDateTime.now()
                : OffsetDateTime.parse(datos.get("check_out_en").toString());

        int fechaId = dimensiones.fechaId(ocurridoEn);
        Long clienteSk = dimensiones.clienteSk(negocioId, clienteId, null);
        // HU-099 criterio 1: el ADR es lo que rinde la habitación, así que el
        // numerador es el alojamiento, no la cuenta entera. Con el total,
        // el minibar y el spa lo inflaban y dejaba de compararse con nada.
        BigDecimal alojamiento = datos.get("alojamiento") == null ? total
                : numero(datos.get("alojamiento"));
        BigDecimal adr = noches == 0 ? alojamiento
                : alojamiento.divide(BigDecimal.valueOf(noches), 4, RoundingMode.HALF_UP);

        hechos.insertarReserva(negocioId, fechaId, ocurridoEn, null, clienteSk, reservaId, tipoRecursoId,
                recursoId, "FINALIZADA", noches, total, consumos, BigDecimal.ZERO, adr);

        // Una estancia es un hecho, pero la ocupación es una pregunta por día:
        // quien entró el lunes y salió el miércoles ocupó dos noches distintas.
        if (datos.get("desde") != null && datos.get("hasta") != null) {
            // La fecha se toma tal como viene en el evento, sin reinterpretarla
            // en la zona del negocio: desde y hasta son los días de la estancia
            // que fijó quien reservó, ya con su offset. Pasarlos por la zona
            // horaria del servicio correría la noche de entrada un día entero.
            ocupacion.aplicar(negocioId, tipoRecursoId, null,
                    OffsetDateTime.parse(datos.get("desde").toString()).toLocalDate(),
                    OffsetDateTime.parse(datos.get("hasta").toString()).toLocalDate(),
                    alojamiento);
        }

        // HU-097 criterio 1. "Unidades" en el patrón Reserva son noches; el
        // patrón no trae bruto/descuento/impuesto por separado a este nivel.
        agregador.aplicar(negocioId, "RESERVA", reservaId, null, fechaLocal.de(negocioId, ocurridoEn),
                "RESERVA", BigDecimal.valueOf(noches), total, BigDecimal.ZERO, BigDecimal.ZERO, total,
                BigDecimal.ZERO, clienteId);
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
            throw new IllegalArgumentException("No se pudo leer el evento estancia_finalizada", e);
        }
    }

    private static DatosDelNegocio contextoDe(UUID negocioId) {
        return new DatosDelNegocio(negocioId, null, "", "RESERVA", Set.of(), Set.of("REPORTES"), Set.of(),
                Set.of());
    }
}
