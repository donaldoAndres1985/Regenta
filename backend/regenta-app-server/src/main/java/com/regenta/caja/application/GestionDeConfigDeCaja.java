package com.regenta.caja.application;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.caja.domain.ConfigDeCaja;
import com.regenta.caja.domain.MetodoPagoCaja;
import com.regenta.caja.infrastructure.ConfigDeCajaRepositorio;
import com.regenta.shared.security.RequierePermiso;
import com.regenta.shared.tenancy.ContextoDeNegocio;
import com.regenta.shared.web.ReglaDeNegocioException;

/** La configuración de caja del negocio (HU-061, HU-139). */
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

    /** HU-139: reemplaza los métodos de pago que el negocio no cuadra al cerrar. */
    @Transactional
    @RequierePermiso("CAJA_TURNO_EDITAR")
    public ConfigCajaDelNegocio fijarMetodosNoCuadrables(SolicitudDeMetodosNoCuadrables solicitud) {
        Set<MetodoPagoCaja> metodos = convertir(solicitud.metodos());
        ConfigDeCaja c = cargar(ContextoDeNegocio.negocioActual());
        c.fijarMetodosNoCuadrables(metodos);
        config.save(c);
        return ConfigCajaDelNegocio.de(c);
    }

    /** La configuración vigente del negocio, para retiros y cierres (uso interno). */
    @Transactional(readOnly = true)
    public ConfigDeCaja delNegocio(UUID negocioId) {
        return cargar(negocioId);
    }

    private ConfigDeCaja cargar(UUID negocioId) {
        return config.findById(negocioId).orElseGet(() -> ConfigDeCaja.porDefecto(negocioId));
    }

    private static Set<MetodoPagoCaja> convertir(List<String> nombres) {
        EnumSet<MetodoPagoCaja> metodos = EnumSet.noneOf(MetodoPagoCaja.class);
        if (nombres == null) {
            return metodos;
        }
        for (String nombre : nombres) {
            try {
                metodos.add(MetodoPagoCaja.valueOf(nombre == null ? "" : nombre.trim()));
            } catch (IllegalArgumentException e) {
                throw new ReglaDeNegocioException("Método de pago desconocido: " + nombre);
            }
        }
        return metodos;
    }

    public record ConfigCajaDelNegocio(BigDecimal retiroMaxSinAutorizacion,
            List<String> metodosNoCuadrables) {
        static ConfigCajaDelNegocio de(ConfigDeCaja c) {
            return new ConfigCajaDelNegocio(c.getRetiroMaxSinAutorizacion(),
                    c.getMetodosNoCuadrables().stream().map(Enum::name).sorted().toList());
        }
    }
}
