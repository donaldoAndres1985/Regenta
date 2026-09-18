package com.regenta.reportes.infra;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
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
import com.regenta.comun.negocio.ContextoDeNegocio;
import com.regenta.comun.negocio.DatosDelNegocio;
import com.regenta.reportes.aplicacion.AgregadorDiario;
import com.regenta.reportes.aplicacion.FechaLocalDelNegocio;
import com.regenta.reportes.aplicacion.ResolverDeDimensiones;

/**
 * Puebla {@code hechos_venta}, una fila por línea (HU-096 criterio 1). Pasa
 * por el Inbox: el mismo evento entregado dos veces no duplica filas —lo que
 * también prueba el criterio 5, "si se reconstruye desde los eventos, queda
 * idéntico": reprocesar no cambia nada.
 *
 * <p>Payload esperado de {@code venta_completada}: {@code {negocio_id, venta_id,
 * numero, bodega_id, cliente_id, usuario_id, canal, fecha, total,
 * lineas:[{producto_id, sku, nombre, cantidad, precio_unitario, descuento_valor,
 * impuesto_valor, total, costo_unitario}]}}.
 */
@Component
public class ConsumidorDeVentasCompletadas {

    private final InboxIdempotente inbox;
    private final ResolverDeDimensiones dimensiones;
    private final EscritorDeHechos hechos;
    private final AgregadorDiario agregador;
    private final FechaLocalDelNegocio fechaLocal;
    private final ObjectMapper json;

    public ConsumidorDeVentasCompletadas(InboxIdempotente inbox, ResolverDeDimensiones dimensiones,
            EscritorDeHechos hechos, AgregadorDiario agregador, FechaLocalDelNegocio fechaLocal,
            ObjectMapper json) {
        this.inbox = inbox;
        this.dimensiones = dimensiones;
        this.hechos = hechos;
        this.agregador = agregador;
        this.fechaLocal = fechaLocal;
        this.json = json;
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "reportes.ventas-completadas", durable = "true"),
            exchange = @Exchange(name = "${regenta.eventos.exchange:regenta.eventos}",
                    type = ExchangeTypes.TOPIC, durable = "true"),
            key = {"venta_completada"}))
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

    @SuppressWarnings("unchecked")
    private void procesar(UUID negocioId, Map<String, Object> datos) {
        UUID ventaId = uuid(datos.get("venta_id"));
        UUID clienteId = uuid(datos.get("cliente_id"));
        UUID usuarioId = uuid(datos.get("usuario_id"));
        UUID bodegaId = uuid(datos.get("bodega_id"));
        String canal = (String) datos.get("canal");
        OffsetDateTime ocurridoEn =
                datos.get("fecha") == null ? OffsetDateTime.now() : OffsetDateTime.parse(datos.get("fecha").toString());
        int fechaId = dimensiones.fechaId(ocurridoEn);
        Long sucursalSk = dimensiones.sucursalSk(negocioId, bodegaId);
        Long clienteSk = dimensiones.clienteSk(negocioId, clienteId, null);
        Long usuarioSk = dimensiones.usuarioSk(negocioId, usuarioId);

        List<Map<String, Object>> lineas = (List<Map<String, Object>>) datos.getOrDefault("lineas", List.of());
        BigDecimal totalUnidades = BigDecimal.ZERO;
        BigDecimal totalBruto = BigDecimal.ZERO;
        BigDecimal totalDescuento = BigDecimal.ZERO;
        BigDecimal totalImpuesto = BigDecimal.ZERO;
        BigDecimal totalNeto = BigDecimal.ZERO;
        BigDecimal totalCosto = BigDecimal.ZERO;
        for (Map<String, Object> l : lineas) {
            UUID productoId = uuid(l.get("producto_id"));
            String sku = (String) l.get("sku");
            String nombre = (String) l.get("nombre");
            Long productoSk = productoId == null ? null
                    : dimensiones.productoSkDesdeSnapshot(negocioId, productoId, sku, nombre);

            BigDecimal cantidad = numero(l.get("cantidad"));
            BigDecimal precioUnitario = numero(l.get("precio_unitario"));
            BigDecimal descuento = numero(l.get("descuento_valor"));
            BigDecimal impuesto = numero(l.get("impuesto_valor"));
            BigDecimal montoNeto = numero(l.get("total"));
            BigDecimal costoUnitario = numero(l.get("costo_unitario"));
            BigDecimal montoBruto = precioUnitario.multiply(cantidad);
            BigDecimal costo = costoUnitario.multiply(cantidad);

            hechos.insertarVenta(negocioId, fechaId, ocurridoEn, sucursalSk, productoSk, clienteSk,
                    usuarioSk, ventaId, canal, cantidad, montoBruto, descuento, impuesto, montoNeto, costo);

            totalUnidades = totalUnidades.add(cantidad);
            totalBruto = totalBruto.add(montoBruto);
            totalDescuento = totalDescuento.add(descuento);
            totalImpuesto = totalImpuesto.add(impuesto);
            totalNeto = totalNeto.add(montoNeto);
            totalCosto = totalCosto.add(costo);
        }

        // HU-097 criterio 1: el agregado del día se actualiza con el mismo cierre,
        // en la misma transacción del Inbox que ya protege hechos_venta.
        agregador.aplicar(negocioId, "VENTA", ventaId, bodegaId, fechaLocal.de(negocioId, ocurridoEn),
                "VENTA_DIRECTA", totalUnidades, totalBruto, totalDescuento, totalImpuesto, totalNeto,
                totalCosto, clienteId);
    }

    private static BigDecimal numero(Object valor) {
        if (valor == null) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(valor.toString());
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
            throw new IllegalArgumentException("No se pudo leer el evento venta_completada", e);
        }
    }

    private static DatosDelNegocio contextoDe(UUID negocioId) {
        return new DatosDelNegocio(negocioId, null, "", "VENTA_DIRECTA", Set.of(), Set.of("REPORTES"),
                Set.of(), Set.of());
    }
}
