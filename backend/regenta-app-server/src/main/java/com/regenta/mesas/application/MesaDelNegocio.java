package com.regenta.mesas.application;

import com.regenta.mesas.api.*;

import java.util.UUID;

import com.regenta.mesas.domain.Mesa;

/**
 * Una mesa tal como la devuelve la API, con su posiciÃ³n en el plano. Si estÃ¡ en
 * una sesiÃ³n viva, {@code sesionId} dice cuÃ¡l â€” dos mesas con el mismo
 * {@code sesionId} estÃ¡n unidas (HU-083 criterio 4) â€” y {@code minutosAbierta} /
 * {@code numComensales} alimentan el cronÃ³metro y el nÂº de comensales del plano
 * en tiempo real (HU-084 criterio 1). El consumo por mesa llega con E12.
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
        UUID sesionId,
        Integer minutosAbierta,
        Integer numComensales) {

    static MesaDelNegocio de(Mesa m) {
        return new MesaDelNegocio(m.getId(), m.getZonaId(), m.getCodigo(), m.getNombre(),
                m.getCapacidad(), m.getForma().name(), m.getEstado().name(),
                m.getPosX(), m.getPosY(), m.getAncho(), m.getAlto(), m.isActiva(),
                null, null, null);
    }

    static MesaDelNegocio de(Mesa m, UUID sesionId, Integer minutosAbierta, Integer numComensales) {
        return new MesaDelNegocio(m.getId(), m.getZonaId(), m.getCodigo(), m.getNombre(),
                m.getCapacidad(), m.getForma().name(), m.getEstado().name(),
                m.getPosX(), m.getPosY(), m.getAncho(), m.getAlto(), m.isActiva(),
                sesionId, minutosAbierta, numComensales);
    }
}
