package com.regenta.alertas.aplicacion;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.alertas.aplicacion.ConsultaDeInventario.LotePorVencer;
import com.regenta.comun.eventos.RegistroDeEventos;
import com.regenta.comun.negocio.ContextoDeNegocio;

/**
 * Las alertas de inventario (HU-093). Traduce los hechos de stock y de lotes a
 * llamadas al motor:
 *
 * <ul>
 *   <li>{@code stock_bajo_minimo} → alerta {@code STOCK_MINIMO} (criterio 1).</li>
 *   <li>El barrido diario publica {@code lote_por_vencer} por cada lote dentro de
 *       la ventana; el consumidor de ese evento genera la alerta
 *       {@code VENCIMIENTO_LOTE} (criterio 2).</li>
 *   <li>{@code stock_normalizado} → resuelve solas las alertas de stock de ese
 *       producto (criterio 4).</li>
 * </ul>
 *
 * La ruta de la alerta de stock lleva al producto y a la sugerencia de compra
 * (criterio 3).
 */
@Service
public class VigilanciaDeInventario {

    /** Ventana por defecto del barrido: el motor afina con la condición de cada regla. */
    private static final int VENTANA_DIAS = 90;

    private final MotorDeAlertas motor;
    private final ConsultaDeInventario inventario;
    private final RegistroDeEventos eventos;

    public VigilanciaDeInventario(MotorDeAlertas motor, ConsultaDeInventario inventario,
            RegistroDeEventos eventos) {
        this.motor = motor;
        this.inventario = inventario;
        this.eventos = eventos;
    }

    /** Criterio 1: el evento de stock bajo genera la alerta según las reglas. */
    @Transactional
    public List<UUID> alStockBajoMinimo(Map<String, Object> datos) {
        UUID productoId = uuid(datos.get("producto_id"));
        Map<String, Object> campos = new LinkedHashMap<>();
        campos.put("producto", texto(datos.get("producto_nombre"), "el producto"));
        campos.put("existencia", numero(datos.get("existencia")));
        campos.put("minimo", numero(datos.get("minimo")));
        campos.put("bodega", texto(datos.get("bodega_nombre"), "la bodega"));
        String ruta = productoId == null ? null : "/inventario/productos/" + productoId;
        campos.put("sugerencia_compra_ruta", "/compras/sugerencias?producto=" + productoId);
        return motor.evaluar(new HechoDeAlerta("STOCK_MINIMO", "Producto", productoId, ruta,
                uuid(datos.get("bodega_id")), campos));
    }

    /** Criterio 4: el stock volvió por encima del mínimo → cierra la alerta. */
    @Transactional
    public int alStockNormalizado(Map<String, Object> datos) {
        return motor.resolverPorEntidad("STOCK_MINIMO", "Producto",
                uuid(datos.get("producto_id")));
    }

    /** Criterio 2, primer paso: el barrido publica un {@code lote_por_vencer} por lote. */
    @Transactional
    public int barrerVencimientos() {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        List<LotePorVencer> lotes = inventario.lotesPorVencer(negocioId, VENTANA_DIAS);
        for (LotePorVencer lote : lotes) {
            eventos.registrar(negocioId, "Lote", lote.loteId(), "lote_por_vencer",
                    payloadLote(negocioId, lote));
        }
        return lotes.size();
    }

    /** Criterio 2, segundo paso: el evento genera la alerta según las reglas. */
    @Transactional
    public List<UUID> alLotePorVencer(Map<String, Object> datos) {
        UUID loteId = uuid(datos.get("lote_id"));
        Map<String, Object> campos = new LinkedHashMap<>();
        campos.put("producto", texto(datos.get("producto_nombre"), "el producto"));
        campos.put("lote", texto(datos.get("lote_codigo"), "el lote"));
        campos.put("dias_para_vencer", numero(datos.get("dias_para_vencer")));
        campos.put("fecha_vencimiento", texto(datos.get("fecha_vencimiento"), ""));
        String ruta = loteId == null ? null : "/inventario/lotes/" + loteId;
        return motor.evaluar(new HechoDeAlerta("VENCIMIENTO_LOTE", "Lote", loteId, ruta,
                null, campos));
    }

    private static Map<String, Object> payloadLote(UUID negocioId, LotePorVencer lote) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("negocio_id", negocioId.toString());
        p.put("lote_id", lote.loteId().toString());
        p.put("producto_id", lote.productoId() == null ? null : lote.productoId().toString());
        p.put("producto_nombre", lote.productoNombre());
        p.put("lote_codigo", lote.codigoLote());
        p.put("dias_para_vencer", lote.diasParaVencer());
        p.put("fecha_vencimiento",
                lote.fechaVencimiento() == null ? null : lote.fechaVencimiento().toString());
        return p;
    }

    private static UUID uuid(Object v) {
        return v == null ? null : UUID.fromString(v.toString());
    }

    private static Object numero(Object v) {
        if (v instanceof Number || v == null) {
            return v;
        }
        try {
            String s = v.toString();
            return s.contains(".") ? Double.parseDouble(s) : Long.parseLong(s);
        } catch (NumberFormatException noEsNumero) {
            return v;
        }
    }

    private static String texto(Object v, String porDefecto) {
        return v == null || v.toString().isBlank() ? porDefecto : v.toString();
    }
}
