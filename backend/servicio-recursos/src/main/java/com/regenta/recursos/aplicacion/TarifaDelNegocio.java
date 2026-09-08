package com.regenta.recursos.aplicacion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.regenta.recursos.domain.Tarifa;

/** Una tarifa, como la ve el administrador (HU-066). */
public record TarifaDelNegocio(
        UUID id,
        UUID tipoRecursoId,
        UUID recursoId,
        String nombre,
        String unidadTiempo,
        BigDecimal precioBase,
        BigDecimal precioPersonaAdicional,
        String moneda,
        LocalDate vigenteDesde,
        LocalDate vigenteHasta,
        List<Integer> diasSemana,
        int estanciaMinima,
        int prioridad,
        boolean activa) {

    static TarifaDelNegocio de(Tarifa t) {
        return new TarifaDelNegocio(t.getId(), t.getTipoRecursoId(), t.getRecursoId(),
                t.getNombre(), t.getUnidadTiempo().name(), t.getPrecioBase(),
                t.getPrecioPersonaAdicional(), t.getMoneda(), t.getVigenteDesde(),
                t.getVigenteHasta(), t.getDiasSemana(), t.getEstanciaMinima(), t.getPrioridad(),
                t.isActiva());
    }
}
