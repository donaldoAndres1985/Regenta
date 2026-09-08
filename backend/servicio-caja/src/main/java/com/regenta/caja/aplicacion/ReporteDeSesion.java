package com.regenta.caja.aplicacion;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import com.regenta.caja.domain.SesionDeCaja;

/** El resumen de un turno de caja (HU-063 criterio 1). */
public record ReporteDeSesion(
        UUID id,
        String numero,
        UUID cajaId,
        String estado,
        boolean descuadrada,
        UUID usuarioAperturaId,
        OffsetDateTime abiertaEn,
        OffsetDateTime cerradaEn,
        BigDecimal montoApertura,
        BigDecimal montoEsperado,
        BigDecimal montoDeclarado,
        BigDecimal diferencia,
        List<TotalPorMetodo> totalesPorMetodo,
        List<MovimientoDelNegocio> movimientos) {

    static ReporteDeSesion de(SesionDeCaja s, List<TotalPorMetodo> totales,
            List<MovimientoDelNegocio> movimientos) {
        return new ReporteDeSesion(s.getId(), s.getNumero(), s.getCajaId(), s.getEstado().name(),
                s.quedoDescuadrada(), s.getUsuarioAperturaId(), s.getAbiertaEn(), s.getCerradaEn(),
                s.getMontoApertura(), s.getMontoEsperado(), s.getMontoDeclarado(),
                s.getDiferencia(), totales, movimientos);
    }
}
