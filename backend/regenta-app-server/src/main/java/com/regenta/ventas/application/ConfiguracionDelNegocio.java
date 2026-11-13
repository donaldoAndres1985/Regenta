package com.regenta.ventas.application;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.shared.tenancy.ContextoDeNegocio;
import com.regenta.shared.security.RequierePermiso;
import com.regenta.usuarios.api.ConfiguracionNegocioApi;

/**
 * La copia local de lo que Ventas necesita de la configuración del negocio
 * (HU-137). El dato maestro vive en servicio-usuarios y llega por
 * {@code configuracion_negocio_actualizada}.
 */
@Service
public class ConfiguracionDelNegocio {

    private final JdbcTemplate jdbc;
    private final ConfiguracionNegocioApi configuracionNegocio;

    public ConfiguracionDelNegocio(JdbcTemplate jdbc) {
        this(jdbc, null);
    }

    @Autowired
    public ConfiguracionDelNegocio(JdbcTemplate jdbc,
            @Autowired(required = false) ConfiguracionNegocioApi configuracionNegocio) {
        this.jdbc = jdbc;
        this.configuracionNegocio = configuracionNegocio;
    }

    /** Guarda la foto: el evento trae la configuración completa, no un parche. */
    @Transactional
    public void guardar(UUID negocioId, BigDecimal montoIdentificarComprador) {
        jdbc.update("""
                INSERT INTO ventas.config_negocio (negocio_id, monto_identificar_comprador)
                VALUES (?, ?)
                ON CONFLICT (negocio_id) DO UPDATE
                   SET monto_identificar_comprador = EXCLUDED.monto_identificar_comprador,
                       actualizado_en = now()
                """, negocioId, montoIdentificarComprador);
    }

    /** Vacío si el negocio no lo configuró: entonces no se exige nada. */
    @Transactional(readOnly = true)
    public Optional<BigDecimal> montoParaIdentificarComprador(UUID negocioId) {
        List<BigDecimal> montos = jdbc.queryForList(
                "SELECT monto_identificar_comprador FROM ventas.config_negocio WHERE negocio_id = ?",
                BigDecimal.class, negocioId);
        return montos.isEmpty() ? Optional.empty() : Optional.ofNullable(montos.get(0));
    }

    /**
     * HU-137 criterio 4: el POS pregunta aquí y no a servicio-usuarios, porque
     * leer la configuración del negocio pide un permiso que quien cobra no
     * suele tener.
     */
    @Transactional(readOnly = true)
    @RequierePermiso("VENTAS_VENTA_CREAR")
    public ReglasDeCobro reglasDeCobro() {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        boolean stockNegativo = configuracionNegocio != null && configuracionNegocio.permiteStockNegativo(negocioId);
        return new ReglasDeCobro(
                montoParaIdentificarComprador(negocioId).orElse(null),
                stockNegativo);
    }
}
