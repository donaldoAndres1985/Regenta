package com.regenta.mesas.aplicacion;

import java.util.UUID;

import com.regenta.mesas.domain.Mesa;

/**
 * Una mesa tal como la devuelve la API, con su posición en el plano. Si está en
 * una sesión viva, {@code sesionId} dice cuál — dos mesas con el mismo
 * {@code sesionId} están unidas (HU-083 criterio 4).
 */
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
        boolean activa,
        UUID sesionId) {

    static MesaDelNegocio de(Mesa m) {
        return de(m, null);
    }

    static MesaDelNegocio de(Mesa m, UUID sesionId) {
        return new MesaDelNegocio(m.getId(), m.getZonaId(), m.getCodigo(), m.getNombre(),
                m.getCapacidad(), m.getForma().name(), m.getEstado().name(),
                m.getPosX(), m.getPosY(), m.getAncho(), m.getAlto(), m.isActiva(), sesionId);
    }
}
