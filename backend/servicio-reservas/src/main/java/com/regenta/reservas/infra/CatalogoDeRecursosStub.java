package com.regenta.reservas.infra;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.regenta.reservas.aplicacion.AnticipoRequerido;
import com.regenta.reservas.aplicacion.CatalogoDeRecursos;
import com.regenta.reservas.aplicacion.CotizacionDeEstadia;
import com.regenta.reservas.aplicacion.CotizacionDeEstadia.NocheCotizada;
import com.regenta.reservas.aplicacion.RecursoReservable;

/**
 * Stub del catálogo de servicio-recursos (HU-069/HU-070). Los tests cargan
 * recursos con {@link #agregar}, bloqueos con {@link #bloquear}, cotizaciones con
 * {@link #cotizacion} y el porcentaje de anticipo con {@link #anticipoPct}; la
 * integración real (REST o réplica por eventos) queda como seguimiento.
 */
@Component
public class CatalogoDeRecursosStub implements CatalogoDeRecursos {

    private static final BigDecimal PRECIO_NOCHE_POR_DEFECTO = new BigDecimal("100000.0000");

    private final Map<UUID, RecursoReservable> recursos = new LinkedHashMap<>();
    private final List<Bloqueo> bloqueos = new ArrayList<>();
    private final Map<UUID, CotizacionDeEstadia> cotizaciones = new LinkedHashMap<>();
    private BigDecimal anticipoPct = BigDecimal.ZERO;
    private UUID politicaPorDefecto;

    private record Bloqueo(UUID recursoId, OffsetDateTime desde, OffsetDateTime hasta) {
        boolean choca(OffsetDateTime d, OffsetDateTime h) {
            return desde.isBefore(h) && hasta.isAfter(d);
        }
    }

    public RecursoReservable agregar(UUID negocioId, UUID tipoRecursoId, String codigo,
            int capacidad, int bufferAntesMin, int bufferDespuesMin) {
        RecursoReservable r = new RecursoReservable(UUID.randomUUID(), tipoRecursoId, codigo,
                "Recurso " + codigo, capacidad, bufferAntesMin, bufferDespuesMin);
        recursos.put(r.id(), r);
        return r;
    }

    public void bloquear(UUID recursoId, OffsetDateTime desde, OffsetDateTime hasta) {
        bloqueos.add(new Bloqueo(recursoId, desde, hasta));
    }

    public void cotizacion(UUID recursoId, CotizacionDeEstadia cotizacion) {
        cotizaciones.put(recursoId, cotizacion);
    }

    public void anticipoPct(BigDecimal fraccion, UUID politicaId) {
        this.anticipoPct = fraccion;
        this.politicaPorDefecto = politicaId;
    }

    public void reiniciar() {
        recursos.clear();
        bloqueos.clear();
        cotizaciones.clear();
        anticipoPct = BigDecimal.ZERO;
        politicaPorDefecto = null;
    }

    @Override
    public List<RecursoReservable> reservables(UUID negocioId, UUID tipoRecursoId) {
        return recursos.values().stream()
                .filter(r -> tipoRecursoId == null || tipoRecursoId.equals(r.tipoRecursoId()))
                .toList();
    }

    @Override
    public List<UUID> recursosBloqueados(UUID negocioId, OffsetDateTime desde,
            OffsetDateTime hasta) {
        return bloqueos.stream().filter(b -> b.choca(desde, hasta)).map(Bloqueo::recursoId)
                .distinct().toList();
    }

    @Override
    public CotizacionDeEstadia cotizar(UUID negocioId, UUID recursoId, LocalDate fechaEntrada,
            int noches, int personas) {
        CotizacionDeEstadia cargada = cotizaciones.get(recursoId);
        if (cargada != null) {
            return cargada;
        }
        List<NocheCotizada> lista = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (int i = 0; i < noches; i++) {
            lista.add(new NocheCotizada(fechaEntrada.plusDays(i), UUID.randomUUID(),
                    PRECIO_NOCHE_POR_DEFECTO));
            total = total.add(PRECIO_NOCHE_POR_DEFECTO);
        }
        return new CotizacionDeEstadia(total, "COP", true, lista);
    }

    @Override
    public AnticipoRequerido anticipo(UUID negocioId, UUID politicaCancelacionId, BigDecimal total,
            OffsetDateTime entrada) {
        UUID politica = politicaCancelacionId != null ? politicaCancelacionId : politicaPorDefecto;
        if (politica == null && anticipoPct.signum() == 0) {
            return AnticipoRequerido.ninguno();
        }
        BigDecimal anticipo = total.multiply(anticipoPct)
                .setScale(4, java.math.RoundingMode.HALF_UP);
        return new AnticipoRequerido(politica, anticipo);
    }
}
