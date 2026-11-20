package com.regenta.inventario.infrastructure;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.regenta.inventario.application.EntradaDeCompras;
import com.regenta.inventario.application.EntradaDeRecepcion;
import com.regenta.shared.events.EventoGenerico;
import com.regenta.shared.events.InboxIdempotente;
import com.regenta.shared.tenancy.ContextoDeNegocio;
import com.regenta.shared.tenancy.DatosDelNegocio;

/**
 * HU-165 · El lado de Inventario de una recepción de Compras: al llegar
 * {@code recepcion_registrada}, da entrada a lo recibido y recalcula el costo promedio
 * ({@link EntradaDeCompras}). Pasa por el Inbox para procesar cada mensaje una sola vez, y corre
 * como el usuario que confirmó la recepción para que el libro mayor diga quién fue.
 */
@Component
public class ConsumidorDeCompras {

    static final String EVENTO = "recepcion_registrada";
    private static final String CONSUMIDOR = "inventario.ConsumidorDeCompras";

    private final InboxIdempotente inbox;
    private final EntradaDeCompras entradas;
    private final ObjectMapper json;

    public ConsumidorDeCompras(InboxIdempotente inbox, EntradaDeCompras entradas,
            ObjectMapper json) {
        this.inbox = inbox;
        this.entradas = entradas;
        this.json = json;
    }

    @ApplicationModuleListener
    public void recibir(EventoGenerico evento) {
        if (evento == null || !EVENTO.equals(evento.tipoEvento())) {
            return;
        }
        String cuerpo = serializar(evento.payload());
        Map<String, Object> datos = leer(cuerpo);
        UUID negocioId = evento.negocioId() != null ? evento.negocioId() : uuid(datos, "negocio_id");
        if (negocioId == null) {
            return;
        }
        UUID usuarioId = uuid(datos, "usuario_id");

        ContextoDeNegocio.en(contextoDe(negocioId, usuarioId), () ->
                inbox.procesarUnaVez(CONSUMIDOR, evento.id(), negocioId, EVENTO, cuerpo,
                        payload -> entradas.registrar(entradaDe(datos))));
    }

    @SuppressWarnings("unchecked")
    static EntradaDeRecepcion entradaDe(Map<String, Object> datos) {
        List<EntradaDeRecepcion.Linea> lineas = ((List<Map<String, Object>>) datos.get("lineas"))
                .stream()
                .map(l -> new EntradaDeRecepcion.Linea(
                        uuid(l, "producto_id"),
                        new BigDecimal(l.get("cantidad").toString()),
                        l.get("costo_unitario") == null ? BigDecimal.ZERO
                                : new BigDecimal(l.get("costo_unitario").toString()),
                        texto(l, "codigo_lote"),
                        l.get("fecha_vencimiento") == null ? null
                                : LocalDate.parse(l.get("fecha_vencimiento").toString()),
                        texto(l, "registro_sanitario")))
                .toList();
        return new EntradaDeRecepcion(uuid(datos, "recepcion_id"), texto(datos, "numero"),
                uuid(datos, "bodega_id"), lineas);
    }

    private static UUID uuid(Map<String, Object> datos, String campo) {
        Object v = datos.get(campo);
        return v == null ? null : UUID.fromString(v.toString());
    }

    private static String texto(Map<String, Object> datos, String campo) {
        Object v = datos.get(campo);
        return v == null ? null : v.toString();
    }

    private String serializar(Object payload) {
        if (payload == null) {
            return "{}";
        }
        if (payload instanceof String s) {
            return s;
        }
        try {
            return json.writeValueAsString(payload);
        } catch (Exception e) {
            throw new IllegalArgumentException("No se pudo serializar el evento de la recepción", e);
        }
    }

    private Map<String, Object> leer(String cuerpo) {
        try {
            return json.readValue(cuerpo, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            throw new IllegalArgumentException("No se pudo leer el evento de la recepción", e);
        }
    }

    private static DatosDelNegocio contextoDe(UUID negocioId, UUID usuarioId) {
        return new DatosDelNegocio(negocioId, usuarioId, "", "VENTA_DIRECTA", Set.of(),
                Set.of("INVENTARIO"), Set.of(), Set.of());
    }
}
