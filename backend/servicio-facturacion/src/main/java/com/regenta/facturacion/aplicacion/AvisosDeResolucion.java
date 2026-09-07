package com.regenta.facturacion.aplicacion;

import java.util.LinkedHashMap;
import java.util.Map;

import com.regenta.facturacion.domain.Resolucion;

/** Payloads de los eventos de resolución, en un solo sitio. */
final class AvisosDeResolucion {

    private AvisosDeResolucion() {
    }

    /** {@code resolucion_por_agotarse}: queda menos del 10% del rango. */
    static Map<String, Object> porAgotarse(Resolucion r) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("negocio_id", r.getNegocioId().toString());
        payload.put("resolucion_id", r.getId().toString());
        payload.put("numero_resolucion", r.getNumeroResolucion());
        payload.put("tipo_documento", r.getTipoDocumento().name());
        payload.put("disponibles", r.numerosDisponibles());
        payload.put("total_rango", r.totalDelRango());
        return payload;
    }
}
