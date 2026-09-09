package com.regenta.menu.infra;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
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
import com.regenta.comun.eventos.RegistroDeEventos;
import com.regenta.comun.negocio.ContextoDeNegocio;
import com.regenta.comun.negocio.DatosDelNegocio;
import com.regenta.menu.aplicacion.ExplosionDeRecetas;
import com.regenta.menu.aplicacion.InsumoAConsumir;
import com.regenta.menu.aplicacion.LineaAExplotar;

/**
 * Al cerrarse una comanda, explota sus líneas contra las recetas y publica
 * {@code insumos_consumidos} para que servicio-inventario descuente stock
 * (HU-079 criterio 3). Pasa por el Inbox: una comanda repetida no descuenta dos
 * veces.
 *
 * <p>Payload esperado de {@code pedido_completado}:
 * {@code {negocio_id, comanda_id, lineas:[{item_menu_id, cantidad, modificador_ids:[...]}]}}.
 */
@Component
public class ConsumidorDePedidos {

    private final InboxIdempotente inbox;
    private final ExplosionDeRecetas explosion;
    private final RegistroDeEventos eventos;
    private final ObjectMapper json;

    public ConsumidorDePedidos(InboxIdempotente inbox, ExplosionDeRecetas explosion,
            RegistroDeEventos eventos, ObjectMapper json) {
        this.inbox = inbox;
        this.explosion = explosion;
        this.eventos = eventos;
        this.json = json;
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "menu.pedidos-completados", durable = "true"),
            exchange = @Exchange(name = "${regenta.eventos.exchange:regenta.eventos}",
                    type = ExchangeTypes.TOPIC, durable = "true"),
            key = {"pedido_completado"}))
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
                mensajeId, negocioId, tipoEvento, cuerpo, payload -> explotar(negocioId, datos)));
    }

    @SuppressWarnings("unchecked")
    private void explotar(UUID negocioId, Map<String, Object> datos) {
        UUID comandaId = datos.get("comanda_id") == null ? null
                : UUID.fromString(datos.get("comanda_id").toString());

        List<Map<String, Object>> lineasCrudas =
                (List<Map<String, Object>>) datos.getOrDefault("lineas", List.of());
        List<LineaAExplotar> lineas = new ArrayList<>();
        for (Map<String, Object> l : lineasCrudas) {
            if (l.get("item_menu_id") == null) {
                continue;
            }
            List<UUID> mods = new ArrayList<>();
            Object crudos = l.get("modificador_ids");
            if (crudos instanceof List<?> lista) {
                for (Object o : lista) {
                    mods.add(UUID.fromString(o.toString()));
                }
            }
            lineas.add(new LineaAExplotar(UUID.fromString(l.get("item_menu_id").toString()),
                    new BigDecimal(l.getOrDefault("cantidad", 1).toString()), mods));
        }

        List<InsumoAConsumir> insumos = explosion.explotarPara(negocioId, lineas);
        if (insumos.isEmpty()) {
            return;
        }

        List<Map<String, Object>> items = new ArrayList<>();
        for (InsumoAConsumir i : insumos) {
            Map<String, Object> it = new java.util.LinkedHashMap<>();
            it.put("producto_id", i.productoId().toString());
            it.put("cantidad", i.cantidad());
            it.put("nombre", i.nombre());
            items.add(it);
        }
        Map<String, Object> payload = new java.util.LinkedHashMap<>();
        payload.put("negocio_id", negocioId.toString());
        payload.put("comanda_id", comandaId == null ? null : comandaId.toString());
        payload.put("origen", "COMANDA");
        payload.put("items", items);
        eventos.registrar(negocioId, "Comanda", comandaId == null ? UUID.randomUUID() : comandaId,
                "insumos_consumidos", payload);
    }

    private Map<String, Object> leer(String cuerpo) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> mapa = json.readValue(cuerpo, Map.class);
            return mapa;
        } catch (Exception e) {
            throw new IllegalArgumentException("No se pudo leer el evento pedido_completado", e);
        }
    }

    private static DatosDelNegocio contextoDe(UUID negocioId) {
        return new DatosDelNegocio(negocioId, null, "", "COMANDA", Set.of(), Set.of("MENU"),
                Set.of(), Set.of());
    }
}
