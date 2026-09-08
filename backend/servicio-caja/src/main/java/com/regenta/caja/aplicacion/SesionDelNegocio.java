package com.regenta.caja.aplicacion;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.regenta.caja.domain.SesionDeCaja;

/** Una sesión de caja con su arqueo (HU-059). */
public record SesionDelNegocio(
        UUID id,
        String numero,
        UUID cajaId,
        String estado,
        UUID usuarioAperturaId,
        OffsetDateTime abiertaEn,
        BigDecimal montoApertura,
        BigDecimal montoEsperado,
        BigDecimal montoDeclarado,
        BigDecimal diferencia,
        OffsetDateTime cerradaEn,
        String observaciones) {

    static SesionDelNegocio de(SesionDeCaja s) {
        return new SesionDelNegocio(s.getId(), s.getNumero(), s.getCajaId(), s.getEstado().name(),
                s.getUsuarioAperturaId(), s.getAbiertaEn(), s.getMontoApertura(),
                s.getMontoEsperado(), s.getMontoDeclarado(), s.getDiferencia(), s.getCerradaEn(),
                s.getObservaciones());
    }
}
