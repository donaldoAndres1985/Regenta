package com.regenta.menu.infrastructure;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.regenta.menu.application.ExplosionDeRecetas;
import com.regenta.menu.application.GestionDeDisponibilidad;
import com.regenta.menu.application.InsumoAConsumir;
import com.regenta.menu.application.LineaAExplotar;
import com.regenta.shared.events.EventoGenerico;
import com.regenta.shared.events.InboxIdempotente;
import com.regenta.shared.events.RegistroDeEventos;
import com.regenta.shared.tenancy.ContextoDeNegocio;
import com.regenta.shared.tenancy.DatosDelNegocio;

/**
 * Al cerrarse una comanda, explota sus líneas contra las recetas y publica
 * {@code insumos_consumidos} para que inventario descuente stock (HU-079 criterio 3).
 * Pasa por el Inbox para no descontar dos veces.
 */
@Component
public class ConsumidorDePedidos {

    private final InboxIdempotente inbox;
    private final ExplosionDeRecetas explosion;
    private final GestionDeDisponibilidad disponibilidad;
    private final RegistroDeEventos eventos;
    private final ObjectMapper json;

    public ConsumidorDePedidos(InboxIdempotente inbox, ExplosionDeRecetas explosion,
            GestionDeDisponibilidad disponibilidad, RegistroDeEventos eventos, ObjectMapper json) {
        this.inbox = inbox;
        this.explosion = explosion;
        this.disponibilidad = disponibilidad;
        this.eventos = eventos;
        this.json = json;
    }

    @ApplicationModuleListener
    public void recibir(EventoGenerico eventoGenerico) {
        if (eventoGenerico == null || !"pedido_completado".equals(eventoGenerico.tipoEvento())) {
            return;
        }

        UUID mensajeId = eventoGenerico.id();
        String tipoEvento = eventoGenerico.tipoEvento();
        String cuerpo = serializarPayload(eventoGenerico.payload());
        Map<String, Object> datos = leer(cuerpo);

        Object negocio = datos.get("negocio_id");
        if (negocio == null) {
            negocio = eventoGenerico.negocioId();
        }
        if (negocio == null) {
            return;
        }
        UUID negocioId = UUID.fromString(negocio.toString());

        ContextoDeNegocio.en(contextoDe(negocioId), () -> inbox.procesarUnaVez("menu.ConsumidorDePedidos", 
                mensajeId, negocioId, tipoEvento, cuerpo, payload -> explotar(negocioId, datos)));
    }

    @SuppressWarnings("unchecked")
    private void explotar(UUID negocioId, Map<String, Object> datos) {
        UUID comandaId = datos.get("comanda_id") == null ? null
                : UUID.fromString(datos.get("comanda_id").toString());

        List<Map<String, Object>> lineasCrudas =
                (List<Map<String, Object>>) datos.getOrDefault("lineas", List.of());
        List<LineaAExplotar> lineas = new ArrayList<>();
        Map<UUID, Integer> vendidasPorItem = new LinkedHashMap<>();
        for (Map<String, Object> l : lineasCrudas) {
            if (l.get("item_menu_id") == null) {
                continue;
            }
            UUID itemId = UUID.fromString(l.get("item_menu_id").toString());
            BigDecimal cantidad = new BigDecimal(l.getOrDefault("cantidad", 1).toString());
            List<UUID> mods = new ArrayList<>();
            Object crudos = l.get("modificador_ids");
            if (crudos instanceof List<?> lista) {
                for (Object o : lista) {
                    mods.add(UUID.fromString(o.toString()));
                }
            }
            lineas.add(new LineaAExplotar(itemId, cantidad, mods));
            vendidasPorItem.merge(itemId, Math.max(cantidad.intValue(), 0), Integer::sum);
        }

        disponibilidad.registrarVentas(negocioId, vendidasPorItem, LocalDate.now());

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

    private String serializarPayload(Object payload) {
        if (payload == null) {
            return "{}";
        }
        if (payload instanceof String s) {
            return s;
        }
        try {
            return json.writeValueAsString(payload);
        } catch (Exception e) {
            throw new IllegalArgumentException("No se pudo serializar el payload", e);
        }
    }

    private Map<String, Object> leer(String cuerpo) {
        try {
            return json.readValue(cuerpo, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            throw new IllegalArgumentException("No se pudo leer el evento pedido_completado", e);
        }
    }

    private static DatosDelNegocio contextoDe(UUID negocioId) {
        return new DatosDelNegocio(negocioId, null, "", "COMANDA", Set.of(), Set.of("MENU"),
                Set.of(), Set.of());
    }
}
