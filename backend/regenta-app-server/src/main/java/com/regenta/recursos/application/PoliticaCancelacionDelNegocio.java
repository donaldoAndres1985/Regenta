package com.regenta.recursos.application;

import java.math.BigDecimal;
import java.util.UUID;

import com.regenta.recursos.domain.PoliticaCancelacion;

/** Una polÃ­tica de cancelaciÃ³n como la ve el administrador (HU-068). */
public record PoliticaCancelacionDelNegocio(
        UUID id,
        String nombre,
        int horasAntes,
        BigDecimal penalizacionPct,
        BigDecimal anticipoRequeridoPct,
        boolean esDefault,
        boolean activa) {

    static PoliticaCancelacionDelNegocio de(PoliticaCancelacion p) {
        return new PoliticaCancelacionDelNegocio(p.getId(), p.getNombre(), p.getHorasAntes(),
                p.getPenalizacionPct(), p.getAnticipoRequeridoPct(), p.isEsDefault(),
                p.isActiva());
    }
}
