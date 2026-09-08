package com.regenta.caja.aplicacion;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.regenta.caja.domain.SesionDeCaja;

/**
 * Una fila del listado de sesiones por rango (HU-063 criterio 2). {@code
 * descuadrada} es lo que el reporte usa para destacarla (criterio 3).
 */
public record SesionEnReporte(
        UUID id,
        String numero,
        UUID cajaId,
        String estado,
        boolean descuadrada,
        UUID usuarioAperturaId,
        OffsetDateTime abiertaEn,
        OffsetDateTime cerradaEn,
        BigDecimal montoEsperado,
        BigDecimal montoDeclarado,
        BigDecimal diferencia) {

    static SesionEnReporte de(SesionDeCaja s) {
        return new SesionEnReporte(s.getId(), s.getNumero(), s.getCajaId(), s.getEstado().name(),
                s.quedoDescuadrada(), s.getUsuarioAperturaId(), s.getAbiertaEn(), s.getCerradaEn(),
                s.getMontoEsperado(), s.getMontoDeclarado(), s.getDiferencia());
    }
}
