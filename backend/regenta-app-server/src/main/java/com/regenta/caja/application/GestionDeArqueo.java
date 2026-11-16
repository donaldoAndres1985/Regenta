package com.regenta.caja.application;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.caja.domain.ArqueoDenominacion;
import com.regenta.caja.domain.ArqueoMetodo;
import com.regenta.caja.domain.ConfigDeCaja;
import com.regenta.caja.domain.EsperadoDeMetodo;
import com.regenta.caja.domain.MetodoPagoCaja;
import com.regenta.caja.domain.SesionDeCaja;
import com.regenta.caja.domain.TipoDenominacion;
import com.regenta.caja.infrastructure.ArqueoDenominacionRepositorio;
import com.regenta.caja.infrastructure.ArqueoMetodoRepositorio;
import com.regenta.caja.infrastructure.MovimientoDeCajaRepositorio;
import com.regenta.caja.infrastructure.SesionDeCajaRepositorio;
import com.regenta.shared.web.ConflictoDeEstadoException;
import com.regenta.shared.web.NoEncontradoException;
import com.regenta.shared.web.RecursoDuplicadoException;
import com.regenta.shared.tenancy.ContextoDeNegocio;
import com.regenta.shared.security.RequierePermiso;

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

    private final GestionDeConfigDeCaja config;
    private final ArqueoMetodoRepositorio arqueoMetodos;

    public GestionDeArqueo(ArqueoDenominacionRepositorio arqueo,
            SesionDeCajaRepositorio sesiones, MovimientoDeCajaRepositorio movimientos,
            GestionDeConfigDeCaja config, ArqueoMetodoRepositorio arqueoMetodos) {
        this.arqueo = arqueo;
        this.sesiones = sesiones;
        this.movimientos = movimientos;
        this.config = config;
        this.arqueoMetodos = arqueoMetodos;
    }

    /**
     * HU-140: el arqueo por método de pago. Con la sesión abierta, es la previa del cierre:
     * el esperado de cada método y, para el efectivo, el conteo por denominaciones si existe.
     * Con la sesión cerrada, es el arqueo guardado al cerrar, congelado (I8b, I12).
     */
    @Transactional(readOnly = true)
    @RequierePermiso("CAJA_TURNO_VER")
    public ArqueoPorMetodo porMetodo(UUID sesionId) {
        SesionDeCaja sesion = delNegocio(sesionId);
        if (!sesion.getEstado().estaAbierta()) {
            return ArqueoPorMetodo.de(arqueoMetodos.findBySesionIdOrderByMetodoPago(sesionId).stream()
                    .map(ArqueoMetodo::comoLinea).toList());
        }

        ConfigDeCaja cfg = config.delNegocio(sesion.getNegocioId());
        BigDecimal efectivoContado = totalContadoDe(sesionId);
        Map<MetodoPagoCaja, EsperadoDeMetodo> esperados = esperadosDe(sesionId);
        List<ArqueoPorMetodo.Linea> lineas = esperados.values().stream().map(e -> {
            boolean cuadrable = cfg.esCuadrable(e.metodo());
            BigDecimal contado = !cuadrable ? e.esperado()
                    : e.metodo() == MetodoPagoCaja.EFECTIVO ? efectivoContado : null;
            return new ArqueoPorMetodo.Linea(e.metodo().name(), cuadrable, e.esperado(), contado,
                    contado == null ? null : contado.subtract(e.esperado()), e.movimientos());
        }).toList();
        return new ArqueoPorMetodo(lineas);
    }

    /** Lo esperado por método de la sesión (I7), con línea de efectivo siempre (I10). */
    @Transactional(readOnly = true)
    public Map<MetodoPagoCaja, EsperadoDeMetodo> esperadosDe(UUID sesionId) {
        Map<MetodoPagoCaja, EsperadoDeMetodo> esperados = new EnumMap<>(MetodoPagoCaja.class);
        esperados.put(MetodoPagoCaja.EFECTIVO,
                new EsperadoDeMetodo(MetodoPagoCaja.EFECTIVO, BigDecimal.ZERO, 0));
        for (TotalPorMetodo t : movimientos.totalesPorMetodo(sesionId)) {
            MetodoPagoCaja metodo = MetodoPagoCaja.valueOf(t.metodo());
            esperados.put(metodo, new EsperadoDeMetodo(metodo, t.total(), t.movimientos()));
        }
        return esperados;
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
