package com.regenta.caja.aplicacion;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.caja.domain.ArqueoDenominacion;
import com.regenta.caja.domain.SesionDeCaja;
import com.regenta.caja.domain.TipoDenominacion;
import com.regenta.caja.infra.ArqueoDenominacionRepositorio;
import com.regenta.caja.infra.MovimientoDeCajaRepositorio;
import com.regenta.caja.infra.SesionDeCajaRepositorio;
import com.regenta.comun.errores.ConflictoDeEstadoException;
import com.regenta.comun.errores.NoEncontradoException;
import com.regenta.comun.errores.RecursoDuplicadoException;
import com.regenta.comun.negocio.ContextoDeNegocio;
import com.regenta.comun.negocio.RequierePermiso;

/**
 * El conteo de efectivo por denominación (HU-062). El cajero ingresa cuántos
 * billetes y monedas de cada valor tiene; el total se calcula solo (criterio 1)
 * y se ve la diferencia contra lo esperado antes de confirmar el cierre
 * (criterio 2). Una denominación no se repite (criterio 3).
 */
@Service
public class GestionDeArqueo {

    private final ArqueoDenominacionRepositorio arqueo;
    private final SesionDeCajaRepositorio sesiones;
    private final MovimientoDeCajaRepositorio movimientos;

    public GestionDeArqueo(ArqueoDenominacionRepositorio arqueo,
            SesionDeCajaRepositorio sesiones, MovimientoDeCajaRepositorio movimientos) {
        this.arqueo = arqueo;
        this.sesiones = sesiones;
        this.movimientos = movimientos;
    }

    @Transactional(readOnly = true)
    @RequierePermiso("CAJA_TURNO_VER")
    public ResumenDeArqueo ver(UUID sesionId) {
        SesionDeCaja sesion = delNegocio(sesionId);
        return resumen(sesion, arqueo.findBySesionIdOrderByDenominacionDesc(sesionId));
    }

    /** Guarda (reemplaza) el conteo y devuelve el total y la diferencia en el momento. */
    @Transactional
    @RequierePermiso("CAJA_TURNO_EDITAR")
    public ResumenDeArqueo guardar(UUID sesionId, SolicitudDeArqueo solicitud) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        SesionDeCaja sesion = delNegocio(sesionId);
        if (!sesion.getEstado().estaAbierta()) {
            throw new ConflictoDeEstadoException("La sesión de caja ya está cerrada");
        }

        Set<BigDecimal> vistas = new HashSet<>();
        List<ArqueoDenominacion> filas = solicitud.denominaciones().stream().map(l -> {
            if (!vistas.add(l.denominacion().stripTrailingZeros())) {
                throw new RecursoDuplicadoException(
                        "La denominación " + l.denominacion() + " está repetida");
            }
            return ArqueoDenominacion.de(negocioId, sesionId, l.denominacion(), tipoDe(l.tipo()),
                    l.cantidad());
        }).toList();

        arqueo.borrarDeLaSesion(sesionId);
        arqueo.flush();
        arqueo.saveAll(filas);
        arqueo.flush();

        return resumen(sesion, arqueo.findBySesionIdOrderByDenominacionDesc(sesionId));
    }

    /** El total contado guardado para una sesión (uso interno del cierre). */
    @Transactional(readOnly = true)
    public BigDecimal totalContadoDe(UUID sesionId) {
        return arqueo.existsBySesionId(sesionId) ? arqueo.totalContado(sesionId) : null;
    }

    private ResumenDeArqueo resumen(SesionDeCaja sesion, List<ArqueoDenominacion> filas) {
        BigDecimal total = filas.stream().map(ArqueoDenominacion::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal esperado = movimientos.efectivoEsperado(sesion.getId());
        return new ResumenDeArqueo(total, esperado, total.subtract(esperado),
                filas.stream().map(ResumenDeArqueo.LineaContada::de).toList());
    }

    private SesionDeCaja delNegocio(UUID sesionId) {
        return sesiones.findByIdAndNegocioId(sesionId, ContextoDeNegocio.negocioActual())
                .orElseThrow(() -> new NoEncontradoException("Esa sesión de caja no existe"));
    }

    private static TipoDenominacion tipoDe(String texto) {
        if (texto == null || texto.isBlank()) {
            return TipoDenominacion.BILLETE;
        }
        return "MONEDA".equalsIgnoreCase(texto.trim()) ? TipoDenominacion.MONEDA
                : TipoDenominacion.valueOf(texto.trim().toUpperCase(Locale.ROOT));
    }
}
