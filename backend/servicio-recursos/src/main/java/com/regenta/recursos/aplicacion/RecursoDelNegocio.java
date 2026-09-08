package com.regenta.recursos.aplicacion;

import java.util.Map;
import java.util.UUID;

import com.regenta.recursos.domain.Recurso;

/** Un recurso individual, como lo ve el administrador (HU-065). */
public record RecursoDelNegocio(
        UUID id,
        UUID tipoRecursoId,
        String codigo,
        String nombre,
        String descripcion,
        UUID sucursalId,
        int capacidad,
        String piso,
        String zona,
        String estado,
        boolean disponible,
        Map<String, Object> atributos,
        String imagenUrl) {

    static RecursoDelNegocio de(Recurso r) {
        return new RecursoDelNegocio(r.getId(), r.getTipoRecursoId(), r.getCodigo(), r.getNombre(),
                r.getDescripcion(), r.getSucursalId(), r.getCapacidad(), r.getPiso(), r.getZona(),
                r.getEstado().name(), r.disponibleParaReservar(), r.getAtributos(),
                r.getImagenUrl());
    }
}
