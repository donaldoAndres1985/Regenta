package com.regenta.reportes.infrastructure;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.modulith.events.ApplicationModuleListener;
import com.regenta.shared.events.EventoGenerico;
import com.fasterxml.jackson.core.type.TypeReference;
import com.regenta.shared.events.InboxIdempotente;
import com.regenta.shared.tenancy.ContextoDeNegocio;
import com.regenta.shared.tenancy.DatosDelNegocio;
import com.regenta.reportes.application.AgregadorDiario;
import com.regenta.reportes.application.FechaLocalDelNegocio;
import com.regenta.reportes.application.ResolverDeDimensiones;

/**
 * Puebla {@code hechos_comanda}, una fila por línea del pedido (HU-096
 * criterio 4, patrón Comanda). Pasa por el Inbox.
 *
 * <p>Payload esperado de {@code pedido_completado}: {@code {negocio_id,
 * comanda_id, mesa_id, usuario_id, propina, tiempo_mesa_min,
 * lineas:[{item_menu_id, nombre, cantidad, monto_neto, costo,
 * tiempo_preparacion_min}]}} (HU-090).
 */
@Component
public class ConsumidorDePedidosCompletados {

    private final InboxIdempotente inbox;
    private final ResolverDeDimensiones dimensiones;
    private final EscritorDeHechos hechos;
    private final AgregadorDiario agregador;
    private final FechaLocalDelNegocio fechaLocal;
    private final ObjectMapper json;

    public ConsumidorDePedidosCompletados(InboxIdempotente inbox, ResolverDeDimensiones dimensiones,
            EscritorDeHechos hechos, AgregadorDiario agregador, FechaLocalDelNegocio fechaLocal,
            ObjectMapper json) {
        this.inbox = inbox;
        this.dimensiones = dimensiones;
        this.hechos = hechos;
        this.agregador = agregador;
        this.fechaLocal = fechaLocal;
        this.json = json;
    }

        private static final Set<String> EVENTOS_SOPORTADOS = Set.of("pedido_completado");

    @ApplicationModuleListener
    public void recibir(EventoGenerico evento) {
        if (evento == null) {
            return;
        }
        String tipoEvento = evento.tipoEvento();
        if (!EVENTOS_SOPORTADOS.contains(tipoEvento)) {
            return;
        }

        UUID mensajeId = evento.id();
        String cuerpo = serializarPayload(evento.payload());
        Map<String, Object> datos = leer(cuerpo);

        Object negocio = datos.get("negocio_id");
        if (negocio == null && evento.negocioId() != null) {
            negocio = evento.negocioId();
        }
        if (negocio == null) {
            return;
        }
        UUID negocioId = UUID.fromString(negocio.toString());
        final UUID nId = negocioId;

        ContextoDeNegocio.en(contextoDe(nId), () -> inbox.procesarUnaVez(mensajeId, nId, tipoEvento, cuerpo, payload -> procesar(nId, datos)));
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
            return "{}";
        }
    }

        private void procesar(UUID negocioId, Map<String, Object> datos) {
        UUID comandaId = uuid(datos.get("comanda_id"));
        UUID usuarioId = uuid(datos.get("usuario_id"));
        // HU-099: la mesa y los comensales son la rotación y el ticket por
        // comensal. Venían en el evento y se estaban tirando.
        UUID mesaId = uuid(datos.get("mesa_id"));
        // HU-134: el código sale de dim_mesa, snapshot al momento del pedido
        // (criterio 3); si la mesa se borra después, este hecho no cambia
        // (criterio 5).
        String mesaCodigo = dimensiones.mesaCodigo(negocioId, mesaId);
        Integer numComensales = entero(datos.get("num_comensales"));
        BigDecimal propina = numeroONulo(datos.get("propina"));
        Integer tiempoMesaMin = entero(datos.get("tiempo_mesa_min"));
        OffsetDateTime ocurridoEn = OffsetDateTime.now();
        int fechaId = dimensiones.fechaId(ocurridoEn);
        Long usuarioSk = dimensiones.usuarioSk(negocioId, usuarioId);

        List<Map<String, Object>> lineas = (List<Map<String, Object>>) datos.getOrDefault("lineas", List.of());
        BigDecimal totalUnidades = BigDecimal.ZERO;
        BigDecimal totalNeto = BigDecimal.ZERO;
        BigDecimal totalCosto = BigDecimal.ZERO;
        for (Map<String, Object> l : lineas) {
            UUID itemMenuId = uuid(l.get("item_menu_id"));
            String nombre = (String) l.get("nombre");
            BigDecimal cantidad = numero(l.get("cantidad"));
            BigDecimal montoNeto = numero(l.get("monto_neto"));
            BigDecimal costo = numero(l.get("costo"));
            Integer tiempoPreparacionMin = entero(l.get("tiempo_preparacion_min"));

            hechos.insertarComanda(negocioId, fechaId, ocurridoEn, null, usuarioSk, comandaId, mesaId,
                    mesaCodigo, numComensales, itemMenuId, nombre, cantidad, montoNeto, costo, propina,
                    tiempoPreparacionMin, tiempoMesaMin);

            totalUnidades = totalUnidades.add(cantidad);
            totalNeto = totalNeto.add(montoNeto);
            totalCosto = totalCosto.add(costo);
        }

        // HU-097 criterio 1. El patrón Comanda no trae bruto/descuento/impuesto
        // por separado a este nivel (ni hechos_comanda los guarda): el bruto del
        // agregado es el neto, sin descuentos ni impuestos que restar.
        agregador.aplicar(negocioId, "COMANDA", comandaId, null, fechaLocal.de(negocioId, ocurridoEn),
                "COMANDA", totalUnidades, totalNeto, BigDecimal.ZERO, BigDecimal.ZERO, totalNeto, totalCosto,
                null);
    }

    private static BigDecimal numero(Object valor) {
        return valor == null ? BigDecimal.ZERO : new BigDecimal(valor.toString());
    }

    private static BigDecimal numeroONulo(Object valor) {
        return valor == null ? null : new BigDecimal(valor.toString());
    }

    private static Integer entero(Object valor) {
        return valor == null ? null : Integer.valueOf(valor.toString());
    }

    private static UUID uuid(Object valor) {
        return valor == null ? null : UUID.fromString(valor.toString());
    }

    private Map<String, Object> leer(String cuerpo) {
        try {
                        Map<String, Object> mapa = json.readValue(cuerpo, new TypeReference<Map<String, Object>>() {});
            return mapa;
        } catch (Exception e) {
            throw new IllegalArgumentException("No se pudo leer el evento pedido_completado", e);
        }
    }

    private static DatosDelNegocio contextoDe(UUID negocioId) {
        return new DatosDelNegocio(negocioId, null, "", "COMANDA", Set.of(), Set.of("REPORTES"), Set.of(),
                Set.of());
    }
}
