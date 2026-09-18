package com.regenta.reportes.aplicacion;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.comun.negocio.ContextoDeNegocio;
import com.regenta.comun.negocio.RequierePermiso;

/**
 * El panel de inicio (HU-097): hoy y el mes en curso, sin tocar las tablas de
 * hechos (criterio 2) — cada consulta lee directo de {@code agregados_diarios},
 * que ya viene pre-sumado por {@link AgregadorDiario}.
 */
@Service
public class PanelDiario {

    private final JdbcTemplate jdbc;
    private final ZonaHorariaDeNegocios zonas;

    public PanelDiario(JdbcTemplate jdbc, ZonaHorariaDeNegocios zonas) {
        this.jdbc = jdbc;
        this.zonas = zonas;
    }

    @Transactional(readOnly = true)
    @RequierePermiso("REPORTES_REPORTE_VER")
    public ResumenDiario hoy() {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        LocalDate hoy = hoyDelNegocio(negocioId);
        return resumenEntre(negocioId, hoy, hoy);
    }

    @Transactional(readOnly = true)
    @RequierePermiso("REPORTES_REPORTE_VER")
    public ResumenDiario mesActual() {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        LocalDate hoy = hoyDelNegocio(negocioId);
        return resumenEntre(negocioId, hoy.withDayOfMonth(1), hoy);
    }

    private LocalDate hoyDelNegocio(UUID negocioId) {
        return LocalDate.now(ZoneId.of(zonas.de(negocioId)));
    }

    private ResumenDiario resumenEntre(UUID negocioId, LocalDate desde, LocalDate hasta) {
        return jdbc.queryForObject("""
                SELECT COALESCE(SUM(num_documentos), 0), COALESCE(SUM(unidades), 0),
                    COALESCE(SUM(monto_bruto), 0), COALESCE(SUM(descuentos), 0),
                    COALESCE(SUM(impuestos), 0), COALESCE(SUM(monto_neto), 0),
                    COALESCE(SUM(costo), 0), COALESCE(SUM(margen), 0)
                FROM reportes.agregados_diarios
                WHERE negocio_id = ? AND fecha BETWEEN ? AND ?
                """,
                (rs, fila) -> {
                    int documentos = rs.getInt(1);
                    BigDecimal montoNeto = rs.getBigDecimal(6);
                    BigDecimal ticketPromedio = documentos == 0 ? BigDecimal.ZERO
                            : montoNeto.divide(BigDecimal.valueOf(documentos), 4, RoundingMode.HALF_UP);
                    return new ResumenDiario(documentos, rs.getBigDecimal(2), rs.getBigDecimal(3),
                            rs.getBigDecimal(4), rs.getBigDecimal(5), montoNeto, rs.getBigDecimal(7),
                            rs.getBigDecimal(8), ticketPromedio);
                },
                negocioId, desde, hasta);
    }
}
