package com.regenta.facturacion.aplicacion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.regenta.facturacion.domain.FormaPago;
import com.regenta.facturacion.domain.LineaFacturable;
import com.regenta.facturacion.domain.LineaFacturable.ImpuestoFacturable;

/**
 * Lo que Facturación necesita de un evento de cierre para emitir. Se lee del
 * payload con tolerancia: distintos emisores nombran el id del origen
 * {@code origen_id}, {@code venta_id}, {@code estancia_id}, {@code pedido_id} o
 * {@code comanda_id}, y los importes pueden venir como número o como texto.
 */
public record SolicitudDeFacturaDesdeEvento(
        UUID negocioId,
        UUID origenId,
        String numeroOrigen,
        UUID sucursalId,
        UUID clienteId,
        String moneda,
        BigDecimal tasaCambio,
        FormaPago formaPago,
        String medioPagoCodigo,
        LocalDate fechaVencimiento,
        BigDecimal propina,
        Map<String, Object> emisor,
        Map<String, Object> cliente,
        List<LineaFacturable> lineas) {

    /** Solo para releer un snapshot que llegó como texto; no configura nada. */
    private static final ObjectMapper JSON = new ObjectMapper();

    @SuppressWarnings("unchecked")
    public static SolicitudDeFacturaDesdeEvento desde(Map<String, Object> p) {
        return new SolicitudDeFacturaDesdeEvento(
                uuid(p.get("negocio_id")),
                // comanda_id es la clave que publica servicio-comandas en
                // pedido_completado; sin ella, una comanda cerrada nunca
                // llegaba a facturarse.
                uuid(primero(p, "origen_id", "venta_id", "estancia_id", "pedido_id", "comanda_id")),
                texto(primero(p, "numero_origen", "numero")),
                uuid(p.get("sucursal_id")),
                uuid(p.get("cliente_id")),
                texto(p.get("moneda")),
                decimal(p.get("tasa_cambio")),
                formaPago(p.get("forma_pago")),
                texto(p.get("medio_pago_codigo")),
                fecha(p.get("fecha_vencimiento")),
                decimal(p.get("propina")),
                mapa(p.get("emisor")),
                mapa(primero(p, "cliente", "adquiriente", "cliente_snapshot")),
                lineas((List<Object>) p.get("lineas")));
    }

    @SuppressWarnings("unchecked")
    private static List<LineaFacturable> lineas(List<Object> crudas) {
        List<LineaFacturable> lista = new ArrayList<>();
        if (crudas == null) {
            return lista;
        }
        for (Object cruda : crudas) {
            Map<String, Object> l = (Map<String, Object>) cruda;
            List<ImpuestoFacturable> impuestos = new ArrayList<>();
            Object impsCrudos = l.get("impuestos");
            if (impsCrudos instanceof List<?> lista2) {
                for (Object ic : lista2) {
                    Map<String, Object> i = (Map<String, Object>) ic;
                    impuestos.add(new ImpuestoFacturable(texto(i.get("codigo")),
                            texto(i.get("nombre")), decimal(i.get("porcentaje")),
                            decimal(i.get("base")), decimal(i.get("valor")),
                            Boolean.TRUE.equals(i.get("es_retencion"))));
                }
            }
            lista.add(new LineaFacturable(texto(l.get("codigo")), texto(l.get("descripcion")),
                    decimal(l.get("cantidad")), texto(l.get("unidad_codigo")),
                    decimal(l.get("precio_unitario")), decimal(l.get("descuento_pct")),
                    decimal(l.get("descuento_valor")), decimal(l.get("base_gravable")),
                    decimal(l.get("total")), impuestos));
        }
        return lista;
    }

    private static Object primero(Map<String, Object> p, String... claves) {
        for (String c : claves) {
            Object v = p.get(c);
            if (v != null) {
                return v;
            }
        }
        return null;
    }

    private static UUID uuid(Object v) {
        return v == null || v.toString().isBlank() ? null : UUID.fromString(v.toString());
    }

    private static String texto(Object v) {
        return v == null ? null : v.toString();
    }

    private static BigDecimal decimal(Object v) {
        return v == null ? null : new BigDecimal(v.toString());
    }

    private static LocalDate fecha(Object v) {
        return v == null || v.toString().isBlank() ? null : LocalDate.parse(v.toString());
    }

    private static FormaPago formaPago(Object v) {
        if (v == null) {
            return FormaPago.CONTADO;
        }
        try {
            return FormaPago.valueOf(v.toString().trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException noExiste) {
            return FormaPago.CONTADO;
        }
    }

    @SuppressWarnings("unchecked")
    /**
     * El snapshot del cliente se guarda como JSONB en el servicio de origen, y
     * segun quien lo publique puede llegar como objeto o como el texto JSON sin
     * parsear. Se aceptan los dos: perder el adquiriente de una factura por una
     * comilla es el peor final posible, y el fallo seria silencioso.
     */
    private static Map<String, Object> mapa(Object v) {
        if (v instanceof Map) {
            return (Map<String, Object>) v;
        }
        if (v instanceof String texto && !texto.isBlank()) {
            try {
                return JSON.readValue(texto, Map.class);
            } catch (Exception noEsJson) {
                return Map.of();
            }
        }
        return Map.of();
    }
}
