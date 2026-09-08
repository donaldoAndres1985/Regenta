package com.regenta.caja.aplicacion;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.caja.domain.ConfigDeCaja;
import com.regenta.caja.infra.ConfigDeCajaRepositorio;
import com.regenta.comun.negocio.ContextoDeNegocio;
import com.regenta.comun.negocio.RequierePermiso;

/** La configuración de caja del negocio (HU-061). */
@Service
public class GestionDeConfigDeCaja {

    private final ConfigDeCajaRepositorio config;

    public GestionDeConfigDeCaja(ConfigDeCajaRepositorio config) {
        this.config = config;
    }

    @Transactional(readOnly = true)
    @RequierePermiso("CAJA_TURNO_VER")
    public ConfigCajaDelNegocio ver() {
        return ConfigCajaDelNegocio.de(cargar(ContextoDeNegocio.negocioActual()));
    }

    @Transactional
    @RequierePermiso("CAJA_TURNO_EDITAR")
    public ConfigCajaDelNegocio fijar(SolicitudDeConfigCaja solicitud) {
        ConfigDeCaja c = cargar(ContextoDeNegocio.negocioActual());
        c.fijarUmbral(solicitud.retiroMaxSinAutorizacion());
        config.save(c);
        return ConfigCajaDelNegocio.de(c);
    }

    /** El umbral vigente, para el registro de retiros (uso interno). */
    @Transactional(readOnly = true)
    public ConfigDeCaja delNegocio(UUID negocioId) {
        return cargar(negocioId);
    }

    private ConfigDeCaja cargar(UUID negocioId) {
        return config.findById(negocioId).orElseGet(() -> ConfigDeCaja.porDefecto(negocioId));
    }

    public record ConfigCajaDelNegocio(BigDecimal retiroMaxSinAutorizacion) {
        static ConfigCajaDelNegocio de(ConfigDeCaja c) {
            return new ConfigCajaDelNegocio(c.getRetiroMaxSinAutorizacion());
        }
    }
}
