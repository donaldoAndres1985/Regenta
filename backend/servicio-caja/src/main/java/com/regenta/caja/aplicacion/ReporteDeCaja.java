package com.regenta.caja.aplicacion;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.caja.domain.EstadoSesionCaja;
import com.regenta.caja.domain.SesionDeCaja;
import com.regenta.caja.infra.MovimientoDeCajaRepositorio;
import com.regenta.caja.infra.SesionDeCajaRepositorio;
import com.regenta.comun.errores.NoEncontradoException;
import com.regenta.comun.errores.ReglaDeNegocioException;
import com.regenta.comun.negocio.ContextoDeNegocio;
import com.regenta.comun.negocio.RequierePermiso;

/**
 * El reporte de cierre de caja para el gerente (HU-063): el resumen de un turno
 * —totales por método, movimientos y diferencia (criterio 1)— y el listado de
 * sesiones de un rango con su estado (criterios 2 y 3).
 */
@Service
public class ReporteDeCaja {

    private final SesionDeCajaRepositorio sesiones;
    private final MovimientoDeCajaRepositorio movimientos;

    public ReporteDeCaja(SesionDeCajaRepositorio sesiones,
            MovimientoDeCajaRepositorio movimientos) {
        this.sesiones = sesiones;
        this.movimientos = movimientos;
    }

    @Transactional(readOnly = true)
    @RequierePermiso("CAJA_TURNO_VER")
    public ReporteDeSesion deSesion(UUID sesionId) {
        SesionDeCaja sesion = sesiones
                .findByIdAndNegocioId(sesionId, ContextoDeNegocio.negocioActual())
                .orElseThrow(() -> new NoEncontradoException("Esa sesión de caja no existe"));
        List<MovimientoDelNegocio> movs = movimientos.findBySesionIdOrderByOcurridoEnAsc(sesionId)
                .stream().map(MovimientoDelNegocio::de).toList();
        return ReporteDeSesion.de(sesion, movimientos.totalesPorMetodo(sesionId), movs);
    }

    @Transactional(readOnly = true)
    @RequierePermiso("CAJA_TURNO_VER")
    public List<SesionEnReporte> sesiones(LocalDate desde, LocalDate hasta, String estado,
            UUID cajaId) {
        if (desde == null || hasta == null || hasta.isBefore(desde)) {
            throw new ReglaDeNegocioException("El rango de fechas no es válido");
        }
        OffsetDateTime inicio = desde.atStartOfDay().toInstant(ZoneOffset.UTC)
                .atOffset(ZoneOffset.UTC);
        OffsetDateTime fin = hasta.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC)
                .atOffset(ZoneOffset.UTC);
        return sesiones.enRango(ContextoDeNegocio.negocioActual(), inicio, fin,
                        estadoDe(estado), cajaId)
                .stream().map(SesionEnReporte::de).toList();
    }

    private static EstadoSesionCaja estadoDe(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        try {
            return EstadoSesionCaja.valueOf(texto.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException noExiste) {
            throw new ReglaDeNegocioException("Estado no válido: " + texto);
        }
    }
}
