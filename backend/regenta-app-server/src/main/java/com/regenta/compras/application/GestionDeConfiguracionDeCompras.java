package com.regenta.compras.application;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.compras.domain.ConfiguracionDeCompras;
import com.regenta.compras.infrastructure.ConfiguracionDeComprasRepositorio;
import com.regenta.shared.security.RequierePermiso;
import com.regenta.shared.tenancy.ContextoDeNegocio;

/**
 * HU-173 · La configuración de compras de cada negocio (I33). Un negocio que nunca la cambió usa los
 * valores por omisión: exige aprobación, sin monto mínimo, tolerancia 5 % (C1).
 */
@Service
public class GestionDeConfiguracionDeCompras {

    private final ConfiguracionDeComprasRepositorio configuraciones;

    public GestionDeConfiguracionDeCompras(ConfiguracionDeComprasRepositorio configuraciones) {
        this.configuraciones = configuraciones;
    }

    @Transactional(readOnly = true)
    @RequierePermiso("COMPRAS_COMPRA_VER")
    public ConfiguracionDeComprasDelNegocio ver() {
        return ConfiguracionDeComprasDelNegocio.de(deNegocio(ContextoDeNegocio.negocioActual()));
    }

    @Transactional
    @RequierePermiso("COMPRAS_CONFIGURAR")
    public ConfiguracionDeComprasDelNegocio cambiar(SolicitudDeConfiguracionDeCompras solicitud) {
        ConfiguracionDeCompras c = deNegocio(ContextoDeNegocio.negocioActual());
        c.cambiar(solicitud.exigeAprobacion(), solicitud.montoMinimoAprobacion(),
                solicitud.toleranciaRecepcionPct());
        return ConfiguracionDeComprasDelNegocio.de(configuraciones.save(c));
    }

    /** Para los demás casos de uso de Compras: la guardada, o la de por omisión (sin guardarla). */
    ConfiguracionDeCompras deNegocio(UUID negocioId) {
        return configuraciones.findById(negocioId).orElseGet(() -> ConfiguracionDeCompras.porOmision(negocioId));
    }
}
