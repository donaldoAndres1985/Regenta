package com.regenta.reportes.infra;

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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.regenta.comun.eventos.InboxIdempotente;
import com.regenta.comun.negocio.ContextoDeNegocio;
import com.regenta.comun.negocio.DatosDelNegocio;

/**
 * Mantiene {@code dim_mesa} (HU-134): qué código y qué zona tiene cada mesa.
 * {@code hechos_comanda.mesa_codigo} lleva NULL desde V1 porque nada publicaba
 * el catálogo de mesas, igual que le pasaba a {@code dim_recurso} antes de
 * HU-099.
 *
 * <p>Los tres eventos del catálogo traen la mesa completa, así que el
 * consumidor hace siempre el mismo upsert y no le importa cuál llegó primero.
 */
@Component
public class ConsumidorDeMesas {

    private final InboxIdempotente inbox;
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;

    public ConsumidorDeMesas(InboxIdempotente inbox, JdbcTemplate jdbc, ObjectMapper json) {
        this.inbox = inbox;
        this.jdbc = jdbc;
        this.json = json;
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "reportes.mesas", durable = "true"),
            exchange = @Exchange(name = "${regenta.eventos.exchange:regenta.eventos}",
                    type = ExchangeTypes.TOPIC, durable = "true"),
            key = {"mesa_creada", "mesa_actualizada", "mesa_eliminada"}))
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
                tipoEvento, cuerpo, payload -> guardar(negocioId, datos)));
    }

    private void guardar(UUID negocioId, Map<String, Object> datos) {
        jdbc.update("""
                INSERT INTO reportes.dim_mesa (negocio_id, mesa_id, zona_id, zona_nombre, codigo,
                    nombre, capacidad, activa, actualizado_en)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, now())
                ON CONFLICT (negocio_id, mesa_id) DO UPDATE SET
                    zona_id = EXCLUDED.zona_id,
                    zona_nombre = EXCLUDED.zona_nombre,
                    codigo = EXCLUDED.codigo,
                    nombre = EXCLUDED.nombre,
                    capacidad = EXCLUDED.capacidad,
                    activa = EXCLUDED.activa,
                    actualizado_en = now()
                """,
                negocioId, uuid(datos.get("mesa_id")), uuid(datos.get("zona_id")),
                texto(datos.get("zona_nombre")), texto(datos.get("codigo")), texto(datos.get("nombre")),
                entero(datos.get("capacidad")), verdadero(datos.get("activa")));
    }

    private static DatosDelNegocio contextoDe(UUID negocioId) {
        return new DatosDelNegocio(negocioId, null, "", "COMANDA", Set.of(), Set.of("REPORTES"),
                Set.of(), Set.of());
    }

    private static UUID uuid(Object valor) {
        return valor == null ? null : UUID.fromString(valor.toString());
    }

    private static String texto(Object valor) {
        return valor == null ? null : valor.toString();
    }

    private static Integer entero(Object valor) {
        return valor == null ? null : Integer.valueOf(valor.toString());
    }

    private static boolean verdadero(Object valor) {
        return Boolean.parseBoolean(String.valueOf(valor));
    }

    private Map<String, Object> leer(String cuerpo) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> mapa = json.readValue(cuerpo, Map.class);
            return mapa;
        } catch (Exception e) {
            throw new IllegalArgumentException("No se pudo leer el evento del catálogo de mesas", e);
        }
    }
}
