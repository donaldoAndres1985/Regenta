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
 * Mantiene {@code dim_recurso}: cuántas habitaciones, canchas o consultorios
 * hay y cuáles siguen contando (HU-099). Es el denominador de la ocupación y
 * del RevPAR — sin él se sabe cuántas noches se vendieron, pero no sobre
 * cuántas.
 *
 * <p>Los tres eventos del catálogo traen el recurso completo, así que el
 * consumidor hace siempre el mismo upsert y no le importa cuál llegó: uno que
 * arranca a mitad de camino queda con la foto correcta sin reproducir la
 * historia.
 */
@Component
public class ConsumidorDeRecursos {

    private final InboxIdempotente inbox;
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;

    public ConsumidorDeRecursos(InboxIdempotente inbox, JdbcTemplate jdbc, ObjectMapper json) {
        this.inbox = inbox;
        this.jdbc = jdbc;
        this.json = json;
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "reportes.recursos", durable = "true"),
            exchange = @Exchange(name = "${regenta.eventos.exchange:regenta.eventos}",
                    type = ExchangeTypes.TOPIC, durable = "true"),
            key = {"recurso_creado", "recurso_actualizado", "recurso_eliminado"}))
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
                INSERT INTO reportes.dim_recurso (negocio_id, recurso_id, sucursal_id, tipo_recurso_id,
                    tipo_recurso_nombre, codigo, nombre, capacidad, estado, activo, disponible,
                    actualizado_en)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, now())
                ON CONFLICT (negocio_id, recurso_id) DO UPDATE SET
                    sucursal_id = EXCLUDED.sucursal_id,
                    tipo_recurso_id = EXCLUDED.tipo_recurso_id,
                    tipo_recurso_nombre = EXCLUDED.tipo_recurso_nombre,
                    codigo = EXCLUDED.codigo,
                    nombre = EXCLUDED.nombre,
                    capacidad = EXCLUDED.capacidad,
                    estado = EXCLUDED.estado,
                    activo = EXCLUDED.activo,
                    disponible = EXCLUDED.disponible,
                    actualizado_en = now()
                """,
                negocioId, uuid(datos.get("recurso_id")), uuid(datos.get("sucursal_id")),
                uuid(datos.get("tipo_recurso_id")), texto(datos.get("tipo_recurso_nombre")),
                texto(datos.get("codigo")), texto(datos.get("nombre")), entero(datos.get("capacidad")),
                texto(datos.get("estado")), verdadero(datos.get("activo")),
                verdadero(datos.get("disponible")));
    }

    private static DatosDelNegocio contextoDe(UUID negocioId) {
        return new DatosDelNegocio(negocioId, null, "", "RESERVA", Set.of(), Set.of("REPORTES"),
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
            throw new IllegalArgumentException("No se pudo leer el evento del catálogo de recursos", e);
        }
    }
}
