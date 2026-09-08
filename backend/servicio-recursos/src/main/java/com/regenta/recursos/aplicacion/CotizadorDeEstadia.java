package com.regenta.recursos.aplicacion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.comun.errores.NoEncontradoException;
import com.regenta.comun.negocio.ContextoDeNegocio;
import com.regenta.comun.negocio.RequierePermiso;
import com.regenta.recursos.domain.Recurso;
import com.regenta.recursos.domain.Tarifa;
import com.regenta.recursos.infra.RecursoRepositorio;
import com.regenta.recursos.infra.TarifaRepositorio;

/**
 * Cotiza una estancia noche por noche (HU-066). Para cada noche, entre las
 * tarifas que aplican gana la de mayor {@code prioridad} (criterio 1); una
 * estancia de varias noches puede cruzar tarifas y cada noche se cobra a la suya
 * (criterio 4). La vigencia, los días de la semana y la estancia mínima acotan
 * qué tarifas entran (criterios 2, 3 y 5).
 */
@Service
public class CotizadorDeEstadia {

    private final RecursoRepositorio recursos;
    private final TarifaRepositorio tarifas;

    public CotizadorDeEstadia(RecursoRepositorio recursos, TarifaRepositorio tarifas) {
        this.recursos = recursos;
        this.tarifas = tarifas;
    }

    @Transactional(readOnly = true)
    @RequierePermiso("RECURSOS_RECURSO_VER")
    public CotizacionDeEstadia cotizar(SolicitudDeCotizacion solicitud) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        Recurso recurso = recursos.findByIdAndNegocioId(solicitud.recursoId(), negocioId)
                .orElseThrow(() -> new NoEncontradoException("Ese recurso no existe"));

        List<Tarifa> candidatas = tarifas.candidatasPara(negocioId, recurso.getId(),
                recurso.getTipoRecursoId());

        BigDecimal total = BigDecimal.ZERO;
        boolean completa = true;
        List<CotizacionDeEstadia.NocheCotizada> noches = new java.util.ArrayList<>();

        for (int i = 0; i < solicitud.noches(); i++) {
            LocalDate fecha = solicitud.fechaEntrada().plusDays(i);
            Tarifa ganadora = candidatas.stream()
                    .filter(t -> t.aplicaEn(fecha, solicitud.noches()))
                    .max(Comparator.comparingInt(Tarifa::getPrioridad)
                            .thenComparing(Tarifa::esPuntual)
                            .thenComparing(t -> t.getId().toString()))
                    .orElse(null);

            if (ganadora == null) {
                completa = false;
                noches.add(new CotizacionDeEstadia.NocheCotizada(fecha, null, null, 0,
                        BigDecimal.ZERO));
                continue;
            }
            BigDecimal precio = ganadora.precioDeNoche(solicitud.personas());
            total = total.add(precio);
            noches.add(new CotizacionDeEstadia.NocheCotizada(fecha, ganadora.getId(),
                    ganadora.getNombre(), ganadora.getPrioridad(), precio));
        }

        String moneda = candidatas.isEmpty() ? "COP" : candidatas.get(0).getMoneda();
        return new CotizacionDeEstadia(recurso.getId(), solicitud.personas(), total, moneda,
                completa, noches);
    }
}
