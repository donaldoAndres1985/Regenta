package com.regenta.recursos.aplicacion;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.comun.errores.NoEncontradoException;
import com.regenta.comun.errores.ReglaDeNegocioException;
import com.regenta.comun.negocio.ContextoDeNegocio;
import com.regenta.comun.negocio.RequierePermiso;
import com.regenta.recursos.domain.Tarifa;
import com.regenta.recursos.domain.UnidadTiempo;
import com.regenta.recursos.infra.RecursoRepositorio;
import com.regenta.recursos.infra.TarifaRepositorio;
import com.regenta.recursos.infra.TipoDeRecursoRepositorio;

/**
 * Alta y mantenimiento de tarifas (HU-066). Una tarifa apunta a un tipo de
 * recurso o a un recurso puntual; la prioridad evita ordenarlas o borrarlas.
 */
@Service
public class GestionDeTarifas {

    private final TarifaRepositorio tarifas;
    private final TipoDeRecursoRepositorio tipos;
    private final RecursoRepositorio recursos;

    public GestionDeTarifas(TarifaRepositorio tarifas, TipoDeRecursoRepositorio tipos,
            RecursoRepositorio recursos) {
        this.tarifas = tarifas;
        this.tipos = tipos;
        this.recursos = recursos;
    }

    @Transactional(readOnly = true)
    @RequierePermiso("RECURSOS_RECURSO_VER")
    public List<TarifaDelNegocio> listar() {
        return tarifas.findByNegocioIdOrderByPrioridadDescNombreAsc(
                        ContextoDeNegocio.negocioActual())
                .stream().map(TarifaDelNegocio::de).toList();
    }

    @Transactional(readOnly = true)
    @RequierePermiso("RECURSOS_RECURSO_VER")
    public TarifaDelNegocio ver(UUID tarifaId) {
        return TarifaDelNegocio.de(delNegocio(tarifaId));
    }

    @Transactional
    @RequierePermiso("RECURSOS_RECURSO_CREAR")
    public TarifaDelNegocio crear(SolicitudDeTarifa solicitud) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        exigirDestino(negocioId, solicitud.tipoRecursoId(), solicitud.recursoId());

        Tarifa tarifa = Tarifa.crear(negocioId, solicitud.tipoRecursoId(), solicitud.recursoId(),
                solicitud.nombre().trim(), unidadDe(solicitud.unidadTiempo()),
                solicitud.precioBase(), solicitud.precioPersonaAdicional(),
                solicitud.vigenteDesde(), solicitud.vigenteHasta(), solicitud.diasSemana(),
                solicitud.horaDesde(), solicitud.horaHasta(),
                valor(solicitud.estanciaMinima(), 1), valor(solicitud.prioridad(), 0));
        tarifas.save(tarifa);
        return TarifaDelNegocio.de(tarifa);
    }

    @Transactional
    @RequierePermiso("RECURSOS_RECURSO_EDITAR")
    public TarifaDelNegocio actualizar(UUID tarifaId, SolicitudDeTarifa solicitud) {
        Tarifa tarifa = delNegocio(tarifaId);
        tarifa.editar(solicitud.nombre().trim(), unidadDe(solicitud.unidadTiempo()),
                solicitud.precioBase(), solicitud.precioPersonaAdicional(),
                solicitud.vigenteDesde(), solicitud.vigenteHasta(), solicitud.diasSemana(),
                solicitud.horaDesde(), solicitud.horaHasta(),
                valor(solicitud.estanciaMinima(), tarifa.getEstanciaMinima()),
                valor(solicitud.prioridad(), tarifa.getPrioridad()));
        tarifas.save(tarifa);
        return TarifaDelNegocio.de(tarifa);
    }

    @Transactional
    @RequierePermiso("RECURSOS_RECURSO_EDITAR")
    public void desactivar(UUID tarifaId) {
        Tarifa tarifa = delNegocio(tarifaId);
        tarifa.desactivar();
        tarifas.save(tarifa);
    }

    private void exigirDestino(UUID negocioId, UUID tipoRecursoId, UUID recursoId) {
        if (tipoRecursoId == null && recursoId == null) {
            throw new ReglaDeNegocioException(
                    "La tarifa necesita un tipo de recurso o un recurso puntual");
        }
        if (tipoRecursoId != null
                && tipos.findByIdAndNegocioId(tipoRecursoId, negocioId).isEmpty()) {
            throw new NoEncontradoException("Ese tipo de recurso no existe");
        }
        if (recursoId != null
                && recursos.findByIdAndNegocioId(recursoId, negocioId).isEmpty()) {
            throw new NoEncontradoException("Ese recurso no existe");
        }
    }

    private Tarifa delNegocio(UUID tarifaId) {
        return tarifas.findByIdAndNegocioId(tarifaId, ContextoDeNegocio.negocioActual())
                .orElseThrow(() -> new NoEncontradoException("Esa tarifa no existe"));
    }

    private static UnidadTiempo unidadDe(String texto) {
        if (texto == null || texto.isBlank()) {
            return UnidadTiempo.NOCHE;
        }
        try {
            return UnidadTiempo.valueOf(texto.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException noExiste) {
            throw new ReglaDeNegocioException("Unidad de tiempo no válida: " + texto);
        }
    }

    private static int valor(Integer v, int porDefecto) {
        return v == null ? porDefecto : v;
    }
}
