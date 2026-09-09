package com.regenta.mesas.aplicacion;

import java.util.UUID;

import com.regenta.mesas.domain.Mesa;

/** Una mesa tal como la devuelve la API, con su posición en el plano. */
public record MesaDelNegocio(
        UUID id,
        UUID zonaId,
        String codigo,
        String nombre,
        int capacidad,
        String forma,
        String estado,
        int posX,
        int posY,
        int ancho,
        int alto,
        boolean activa) {

    static MesaDelNegocio de(Mesa m) {
        return new MesaDelNegocio(m.getId(), m.getZonaId(), m.getCodigo(), m.getNombre(),
                m.getCapacidad(), m.getForma().name(), m.getEstado().name(),
                m.getPosX(), m.getPosY(), m.getAncho(), m.getAlto(), m.isActiva());
    }
}
