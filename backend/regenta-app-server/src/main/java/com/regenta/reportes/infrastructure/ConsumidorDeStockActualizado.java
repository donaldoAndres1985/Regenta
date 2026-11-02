package com.regenta.reportes.infrastructure;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
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
import com.regenta.reportes.application.ResolverDeDimensiones;

/**
 * Puebla {@code hechos_inventario} (HU-098 criterio 3, soporte): sin esto, el
 * reporte de rotación no tiene de dónde sacar el último movimiento de cada
 * producto. Pasa por el Inbox, igual que el resto.
 *
 * <p>Payload esperado de {@code stock_actualizado} (HU-030): {@code
 * {negocio_id, producto_id, bodega_id, tipo, cantidad, saldo_posterior}}. No
 * trae nombre ni SKU: si es la primera vez que este servicio ve el producto,
 * abre una versión mínima —{@link ResolverDeDimensiones#productoSkDesdeSnapshot}
 * hace lo mismo que ya hace {@code ConsumidorDeVentasCompletadas}—, sujeta a
 * que {@code producto_actualizado} la complete después.
 */
@Component
public class ConsumidorDeStockActualizado {

    private final InboxIdempotente inbox;
    private final ResolverDeDimensiones dimensiones;
    private final EscritorDeHechos hechos;
    private final ObjectMapper json;

    public ConsumidorDeStockActualizado(InboxIdempotente inbox, ResolverDeDimensiones dimensiones,
            EscritorDeHechos hechos, ObjectMapper json) {
        this.inbox = inbox;
        this.dimensiones = dimensiones;
        this.hechos = hechos;
        this.json = json;
    }

        private static final Set<String> EVENTOS_SOPORTADOS = Set.of("stock_actualizado");

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
        UUID productoId = uuid(datos.get("producto_id"));
        UUID bodegaId = uuid(datos.get("bodega_id"));
        String tipoMovimiento = (String) datos.get("tipo");
        BigDecimal cantidad = numero(datos.get("cantidad"));
        BigDecimal saldoPosterior = numero(datos.get("saldo_posterior"));
        OffsetDateTime ocurridoEn = OffsetDateTime.now();
        int fechaId = dimensiones.fechaId(ocurridoEn);
        Long productoSk = productoId == null ? null
                : dimensiones.productoSkDesdeSnapshot(negocioId, productoId, null, "(sin nombre todavía)");

        hechos.insertarInventario(negocioId, fechaId, ocurridoEn, productoSk, bodegaId, tipoMovimiento,
                cantidad, BigDecimal.ZERO, saldoPosterior);
    }

    private static BigDecimal numero(Object valor) {
        return valor == null ? BigDecimal.ZERO : new BigDecimal(valor.toString());
    }

    private static UUID uuid(Object valor) {
        return valor == null ? null : UUID.fromString(valor.toString());
    }

    private Map<String, Object> leer(String cuerpo) {
        try {
                        Map<String, Object> mapa = json.readValue(cuerpo, new TypeReference<Map<String, Object>>() {});
            return mapa;
        } catch (Exception e) {
            throw new IllegalArgumentException("No se pudo leer el evento stock_actualizado", e);
        }
    }

    private static DatosDelNegocio contextoDe(UUID negocioId) {
        return new DatosDelNegocio(negocioId, null, "", "VENTA_DIRECTA", Set.of(), Set.of("REPORTES"),
                Set.of(), Set.of());
    }
}
