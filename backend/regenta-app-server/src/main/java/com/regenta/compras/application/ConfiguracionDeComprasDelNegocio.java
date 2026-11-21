package com.regenta.compras.application;

import java.math.BigDecimal;

import com.regenta.compras.domain.ConfiguracionDeCompras;

/** HU-173 · La configuración de compras del negocio, como la ve la app. */
public record ConfiguracionDeComprasDelNegocio(
        boolean exigeAprobacion,
        BigDecimal montoMinimoAprobacion,
        BigDecimal toleranciaRecepcionPct) {

    static ConfiguracionDeComprasDelNegocio de(ConfiguracionDeCompras c) {
        return new ConfiguracionDeComprasDelNegocio(c.isExigeAprobacion(), c.getMontoMinimoAprobacion(),
                c.getToleranciaRecepcionPct());
    }
}
