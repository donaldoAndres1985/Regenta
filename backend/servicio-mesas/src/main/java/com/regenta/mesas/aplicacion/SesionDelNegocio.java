package com.regenta.mesas.aplicacion;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import com.regenta.mesas.domain.SesionDeMesa;

/**
 * Una sesión de mesa tal como la devuelve la API. Para una sesión cerrada,
 * {@code duracionMin} y {@code cerradaEn} dicen cuánto duró; {@code numComensales}
 * dice cuántos fueron (HU-082 criterio 5). Para una abierta, {@code minutosAbierta}
 * es el cronómetro en curso (criterio 1). {@code mesaIds} son todas las mesas del
 * grupo, la principal primero (HU-083 criterio 1).
 */
public record SesionDelNegocio(
        UUID id,
        UUID mesaPrincipalId,
        List<UUID> mesaIds,
        UUID comandaId,
        UUID meseroUsuarioId,
        int numComensales,
        String estado,
        OffsetDateTime abiertaEn,
        OffsetDateTime cerradaEn,
        Integer duracionMin,
        long minutosAbierta) {

    static SesionDelNegocio de(SesionDeMesa s, List<UUID> mesaIds) {
        return new SesionDelNegocio(s.getId(), s.getMesaPrincipalId(), mesaIds, s.getComandaId(),
                s.getMeseroUsuarioId(), s.getNumComensales(), s.getEstado().name(),
                s.getAbiertaEn(), s.getCerradaEn(), s.getDuracionMin(), s.minutosTranscurridos());
    }
}
