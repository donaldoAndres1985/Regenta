package com.regenta.ventas.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.regenta.ventas.domain.VentaLinea;

/**
 * Cómo viaja una línea vendida en {@code venta_completada}. Reportes lee {@code sku},
 * {@code nombre}, {@code precio_unitario} (con IVA) e {@code impuesto_valor}; Facturación
 * (HU-149 C3) lee {@code codigo}, {@code descripcion}, {@code precio_unitario_sin_iva},
 * {@code base_gravable} e {@code impuestos}.
 *
 * <p>Tributo DIAN de IVA = {@code 01}. Exento sale como IVA 0 %; excluido y negocio no
 * responsable no llevan tributo (I1, I3 de {@code docs/dominio/inventario.md}).
 */
public final class LineaVendida {

    private static final String TRIBUTO_IVA = "01";

    private LineaVendida() {
    }

    public static Map<String, Object> comoPayload(VentaLinea l) {
        Map<String, Object> lp = new LinkedHashMap<>();
        lp.put("producto_id", l.getProductoId().toString());
        lp.put("sku", l.getSkuSnapshot());
        lp.put("nombre", l.getNombreSnapshot());
        lp.put("cantidad", l.getCantidad());
        lp.put("precio_unitario", l.getPrecioUnitario());
        lp.put("descuento_valor", l.getDescuentoValor());
        lp.put("impuesto_valor", l.getImpuestoValor());
        lp.put("total", l.getTotal());
        lp.put("costo_unitario", l.getCostoUnitarioSnapshot());
        // HU-149 C3: lo que necesita la factura.
        lp.put("codigo", l.getSkuSnapshot());
        lp.put("descripcion", l.getNombreSnapshot());
        lp.put("unidad_codigo", l.getUnidadSnapshot());
        lp.put("precio_unitario_sin_iva",
                l.getSubtotal().divide(l.getCantidad(), 4, RoundingMode.HALF_UP));
        lp.put("descuento_pct", l.getDescuentoPct());
        lp.put("base_gravable", l.getBaseGravable());
        lp.put("impuestos", impuestos(l));
        return lp;
    }

    private static List<Map<String, Object>> impuestos(VentaLinea l) {
        String codigo = l.getImpuestoCodigo();
        if (codigo == null || "EXCLUIDO".equals(codigo) || PrecioDeLinea.NO_RESPONSABLE.equals(codigo)) {
            return List.of();
        }
        Map<String, Object> iva = new LinkedHashMap<>();
        iva.put("codigo", TRIBUTO_IVA);
        iva.put("nombre", "IVA");
        iva.put("porcentaje", l.getImpuestoPct() == null ? BigDecimal.ZERO : l.getImpuestoPct());
        iva.put("base", l.getBaseGravable());
        iva.put("valor", l.getImpuestoValor());
        iva.put("es_retencion", false);
        return List.of(iva);
    }
}
