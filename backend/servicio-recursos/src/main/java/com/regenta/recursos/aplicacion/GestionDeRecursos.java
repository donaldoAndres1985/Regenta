package com.regenta.recursos.aplicacion;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.comun.errores.ConflictoDeEstadoException;
import com.regenta.comun.errores.NoEncontradoException;
import com.regenta.comun.errores.RecursoDuplicadoException;
import com.regenta.comun.errores.ReglaDeNegocioException;
import com.regenta.comun.eventos.RegistroDeEventos;
import com.regenta.comun.negocio.ContextoDeNegocio;
import com.regenta.comun.negocio.RequierePermiso;
import com.regenta.recursos.domain.EstadoRecurso;
import com.regenta.recursos.domain.Recurso;
import com.regenta.recursos.domain.TipoDeRecurso;
import com.regenta.recursos.infra.AtributoTipoRecursoRepositorio;
import com.regenta.recursos.infra.RecursoRepositorio;
import com.regenta.recursos.infra.TipoDeRecursoRepositorio;

/**
 * Recursos individuales: cada habitación, cancha o consultorio (HU-065). El
 * `codigo` es único por negocio (criterio 1); un recurso en `MANTENIMIENTO` sale
 * de los disponibles (criterio 2); no se borra si tiene reservas futuras
 * (criterio 3); sus `atributos` se validan contra su tipo (criterio 4).
 */
@Service
public class GestionDeRecursos {

    private final RecursoRepositorio recursos;
    private final TipoDeRecursoRepositorio tipos;
    private final AtributoTipoRecursoRepositorio atributos;
    private final ValidadorDeAtributosDeRecurso validador;
    private final ConsultaDeReservasDeRecurso reservas;
    private final RegistroDeEventos eventos;

    public GestionDeRecursos(RecursoRepositorio recursos, TipoDeRecursoRepositorio tipos,
            AtributoTipoRecursoRepositorio atributos, ValidadorDeAtributosDeRecurso validador,
            ConsultaDeReservasDeRecurso reservas, RegistroDeEventos eventos) {
        this.recursos = recursos;
        this.tipos = tipos;
        this.atributos = atributos;
        this.validador = validador;
        this.reservas = reservas;
        this.eventos = eventos;
    }

    @Transactional(readOnly = true)
    @RequierePermiso("RECURSOS_RECURSO_VER")
    public List<RecursoDelNegocio> listar(boolean soloDisponibles) {
        return recursos.findByNegocioIdAndEliminadoEnIsNullOrderByCodigoAsc(
                        ContextoDeNegocio.negocioActual())
                .stream()
                .filter(r -> !soloDisponibles || r.disponibleParaReservar())
                .map(RecursoDelNegocio::de).toList();
    }

    @Transactional(readOnly = true)
    @RequierePermiso("RECURSOS_RECURSO_VER")
    public RecursoDelNegocio ver(UUID recursoId) {
        return RecursoDelNegocio.de(delNegocio(recursoId));
    }

    @Transactional
    @RequierePermiso("RECURSOS_RECURSO_CREAR")
    public RecursoDelNegocio crear(SolicitudDeRecurso solicitud) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        String codigo = solicitud.codigo().trim();

        TipoDeRecurso tipo = tipos.findByIdAndNegocioId(solicitud.tipoRecursoId(), negocioId)
                .orElseThrow(() -> new NoEncontradoException("Ese tipo de recurso no existe"));
        if (recursos.existsByNegocioIdAndCodigo(negocioId, codigo)) {
            throw new RecursoDuplicadoException("Ya hay un recurso con el código " + codigo);
        }
        Map<String, Object> validos = validador.validar(
                atributos.findByTipoRecursoIdOrderByOrdenAscNombreCampoAsc(tipo.getId()),
                solicitud.atributos());

        Recurso recurso = Recurso.crear(negocioId, solicitud.sucursalId(), tipo.getId(), codigo,
                solicitud.nombre().trim(), limpiar(solicitud.descripcion()),
                solicitud.capacidad() == null ? tipo.getCapacidadDefault() : solicitud.capacidad(),
                limpiar(solicitud.piso()), limpiar(solicitud.zona()), validos,
                limpiar(solicitud.imagenUrl()));
        recursos.save(recurso);
        publicar(recurso, tipo, "recurso_creado");
        return RecursoDelNegocio.de(recurso);
    }

    @Transactional
    @RequierePermiso("RECURSOS_RECURSO_EDITAR")
    public RecursoDelNegocio actualizar(UUID recursoId, SolicitudDeRecurso solicitud) {
        Recurso recurso = delNegocio(recursoId);
        Map<String, Object> validos = validador.validar(
                atributos.findByTipoRecursoIdOrderByOrdenAscNombreCampoAsc(recurso.getTipoRecursoId()),
                solicitud.atributos());
        recurso.editar(solicitud.sucursalId(), solicitud.nombre().trim(),
                limpiar(solicitud.descripcion()),
                solicitud.capacidad() == null ? recurso.getCapacidad() : solicitud.capacidad(),
                limpiar(solicitud.piso()), limpiar(solicitud.zona()), validos,
                limpiar(solicitud.imagenUrl()));
        recursos.save(recurso);
        publicar(recurso, "recurso_actualizado");
        return RecursoDelNegocio.de(recurso);
    }

    /** Criterio 2: pasar a MANTENIMIENTO (o cualquier no-DISPONIBLE) lo saca de disponibles. */
    @Transactional
    @RequierePermiso("RECURSOS_RECURSO_EDITAR")
    public RecursoDelNegocio cambiarEstado(UUID recursoId, String estado) {
        Recurso recurso = delNegocio(recursoId);
        recurso.cambiarEstado(estadoDe(estado));
        recursos.save(recurso);
        publicar(recurso, "recurso_actualizado");
        return RecursoDelNegocio.de(recurso);
    }

    /** Criterio 3: no se elimina si tiene reservas futuras. */
    @Transactional
    @RequierePermiso("RECURSOS_RECURSO_ELIMINAR")
    public void eliminar(UUID recursoId) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        Recurso recurso = delNegocio(recursoId);
        if (reservas.tieneReservasFuturas(negocioId, recursoId)) {
            throw new ConflictoDeEstadoException(
                    "El recurso tiene reservas futuras: cancélalas o reasígnalas antes de eliminarlo");
        }
        recurso.eliminar();
        recursos.save(recurso);
        publicar(recurso, "recurso_eliminado");
    }

    /**
     * El catálogo sale al bus (HU-099). Reportes necesita saber cuántos
     * recursos hay y cuáles están fuera de servicio: sin ese denominador no
     * existe la ocupación, y ningún evento del patrón Reserva lo traía.
     *
     * <p>Va el recurso completo en cada cambio, no un delta: quien lo consume
     * hace un upsert y queda con la foto correcta sin haber visto los eventos
     * anteriores.
     */
    private void publicar(Recurso recurso, String tipoEvento) {
        publicar(recurso, tipos.findByIdAndNegocioId(recurso.getTipoRecursoId(),
                recurso.getNegocioId()).orElse(null), tipoEvento);
    }

    private void publicar(Recurso recurso, TipoDeRecurso tipo, String tipoEvento) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("negocio_id", recurso.getNegocioId().toString());
        payload.put("recurso_id", recurso.getId().toString());
        payload.put("sucursal_id",
                recurso.getSucursalId() == null ? null : recurso.getSucursalId().toString());
        payload.put("tipo_recurso_id", recurso.getTipoRecursoId().toString());
        payload.put("tipo_recurso_nombre", tipo == null ? null : tipo.getNombre());
        payload.put("codigo", recurso.getCodigo());
        payload.put("nombre", recurso.getNombre());
        payload.put("capacidad", recurso.getCapacidad());
        payload.put("estado", recurso.getEstado().name());
        // Un recurso eliminado deja de contar para la ocupacion; uno en
        // MANTENIMIENTO sigue existiendo pero no se puede vender. Son cosas
        // distintas y por eso viajan las dos.
        payload.put("activo", recurso.isActivo() && !recurso.estaEliminado());
        payload.put("disponible", recurso.disponibleParaReservar());
        eventos.registrar(recurso.getNegocioId(), "Recurso", recurso.getId(), tipoEvento, payload);
    }

    private Recurso delNegocio(UUID recursoId) {
        Recurso r = recursos.findByIdAndNegocioId(recursoId, ContextoDeNegocio.negocioActual())
                .orElseThrow(() -> new NoEncontradoException("Ese recurso no existe"));
        if (r.estaEliminado()) {
            throw new NoEncontradoException("Ese recurso no existe");
        }
        return r;
    }

    private static EstadoRecurso estadoDe(String texto) {
        if (texto == null || texto.isBlank()) {
            throw new ReglaDeNegocioException("Indica el estado");
        }
        try {
            return EstadoRecurso.valueOf(texto.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException noExiste) {
            throw new ReglaDeNegocioException("Estado de recurso no válido: " + texto);
        }
    }

    private static String limpiar(String v) {
        if (v == null) {
            return null;
        }
        String t = v.trim();
        return t.isEmpty() ? null : t;
    }
}
