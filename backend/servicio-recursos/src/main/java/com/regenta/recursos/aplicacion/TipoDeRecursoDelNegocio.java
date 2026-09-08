package com.regenta.recursos.aplicacion;

import java.util.List;
import java.util.UUID;

import com.regenta.recursos.domain.TipoDeRecurso;

/** Un tipo de recurso con sus atributos (HU-064). */
public record TipoDeRecursoDelNegocio(
        UUID id,
        String nombre,
        String descripcion,
        String unidadTiempo,
        int duracionMinimaMin,
        int incrementoMin,
        int capacidadDefault,
        boolean permiteOverbooking,
        int bufferAntesMin,
        int bufferDespuesMin,
        boolean activo,
        List<AtributoDelTipo> atributos) {

    static TipoDeRecursoDelNegocio de(TipoDeRecurso t, List<AtributoDelTipo> atributos) {
        return new TipoDeRecursoDelNegocio(t.getId(), t.getNombre(), t.getDescripcion(),
                t.getUnidadTiempo().name(), t.getDuracionMinimaMin(), t.getIncrementoMin(),
                t.getCapacidadDefault(), t.isPermiteOverbooking(), t.getBufferAntesMin(),
                t.getBufferDespuesMin(), t.isActivo(), atributos);
    }
}
