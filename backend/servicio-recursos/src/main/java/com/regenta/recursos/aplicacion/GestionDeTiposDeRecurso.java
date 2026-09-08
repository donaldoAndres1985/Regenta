package com.regenta.recursos.aplicacion;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.comun.errores.NoEncontradoException;
import com.regenta.comun.errores.RecursoDuplicadoException;
import com.regenta.comun.errores.ReglaDeNegocioException;
import com.regenta.comun.negocio.ContextoDeNegocio;
import com.regenta.comun.negocio.RequierePermiso;
import com.regenta.recursos.domain.AtributoTipoRecurso;
import com.regenta.recursos.domain.TipoAtributoRecurso;
import com.regenta.recursos.domain.TipoDeRecurso;
import com.regenta.recursos.domain.UnidadTiempo;
import com.regenta.recursos.infra.AtributoTipoRecursoRepositorio;
import com.regenta.recursos.infra.TipoDeRecursoRepositorio;

/**
 * Tipos de recurso y sus atributos configurables (HU-064). Espejo de
 * {@code GestionDeCategorias} + {@code GestionDeAtributos} de Inventario: una
 * habitación, una cancha y un consultorio usan el mismo sistema, cambian filas
 * de configuración, no código.
 */
@Service
public class GestionDeTiposDeRecurso {

    private final TipoDeRecursoRepositorio tipos;
    private final AtributoTipoRecursoRepositorio atributos;

    public GestionDeTiposDeRecurso(TipoDeRecursoRepositorio tipos,
            AtributoTipoRecursoRepositorio atributos) {
        this.tipos = tipos;
        this.atributos = atributos;
    }

    @Transactional(readOnly = true)
    @RequierePermiso("RECURSOS_RECURSO_VER")
    public List<TipoDeRecursoDelNegocio> listar() {
        return tipos.findByNegocioIdAndActivoTrueOrderByNombreAsc(ContextoDeNegocio.negocioActual())
                .stream().map(t -> TipoDeRecursoDelNegocio.de(t, atributosDe(t.getId()))).toList();
    }

    @Transactional(readOnly = true)
    @RequierePermiso("RECURSOS_RECURSO_VER")
    public TipoDeRecursoDelNegocio ver(UUID tipoId) {
        TipoDeRecurso t = delNegocio(tipoId);
        return TipoDeRecursoDelNegocio.de(t, atributosDe(tipoId));
    }

    @Transactional
    @RequierePermiso("RECURSOS_RECURSO_CREAR")
    public TipoDeRecursoDelNegocio crear(SolicitudDeTipoDeRecurso solicitud) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        String nombre = solicitud.nombre().trim();
        if (tipos.existsByNegocioIdAndNombre(negocioId, nombre)) {
            throw new RecursoDuplicadoException("Ya hay un tipo de recurso '" + nombre + "'");
        }
        TipoDeRecurso t = TipoDeRecurso.crear(negocioId, nombre, limpiar(solicitud.descripcion()),
                unidadDe(solicitud.unidadTiempo()), valor(solicitud.duracionMinimaMin(), 60),
                valor(solicitud.incrementoMin(), 30), valor(solicitud.capacidadDefault(), 1),
                Boolean.TRUE.equals(solicitud.permiteOverbooking()),
                valor(solicitud.bufferAntesMin(), 0), valor(solicitud.bufferDespuesMin(), 0));
        tipos.save(t);
        return TipoDeRecursoDelNegocio.de(t, List.of());
    }

    @Transactional
    @RequierePermiso("RECURSOS_RECURSO_EDITAR")
    public TipoDeRecursoDelNegocio actualizar(UUID tipoId, SolicitudDeTipoDeRecurso solicitud) {
        TipoDeRecurso t = delNegocio(tipoId);
        t.editar(solicitud.nombre().trim(), limpiar(solicitud.descripcion()),
                unidadDe(solicitud.unidadTiempo()),
                valor(solicitud.duracionMinimaMin(), t.getDuracionMinimaMin()),
                valor(solicitud.incrementoMin(), t.getIncrementoMin()),
                valor(solicitud.capacidadDefault(), t.getCapacidadDefault()),
                Boolean.TRUE.equals(solicitud.permiteOverbooking()),
                valor(solicitud.bufferAntesMin(), t.getBufferAntesMin()),
                valor(solicitud.bufferDespuesMin(), t.getBufferDespuesMin()));
        tipos.save(t);
        return TipoDeRecursoDelNegocio.de(t, atributosDe(tipoId));
    }

    @Transactional
    @RequierePermiso("RECURSOS_RECURSO_EDITAR")
    public void desactivar(UUID tipoId) {
        TipoDeRecurso t = delNegocio(tipoId);
        t.desactivar();
        tipos.save(t);
    }

    @Transactional(readOnly = true)
    @RequierePermiso("RECURSOS_RECURSO_VER")
    public List<AtributoDelTipo> atributos(UUID tipoId) {
        delNegocio(tipoId);
        return atributosDe(tipoId);
    }

    /** Criterio 1: definir un atributo que funciona igual que los de categoría. */
    @Transactional
    @RequierePermiso("RECURSOS_RECURSO_EDITAR")
    public AtributoDelTipo agregarAtributo(UUID tipoId, SolicitudDeAtributoDeRecurso solicitud) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        delNegocio(tipoId);
        String campo = solicitud.nombreCampo().trim();
        TipoAtributoRecurso tipoAttr = tipoAtributoDe(solicitud.tipo());
        if (atributos.existsByTipoRecursoIdAndNombreCampo(tipoId, campo)) {
            AtributoTipoRecurso existente = atributos
                    .findByTipoRecursoIdOrderByOrdenAscNombreCampoAsc(tipoId).stream()
                    .filter(a -> a.getNombreCampo().equals(campo)).findFirst().orElseThrow();
            existente.configurar(solicitud.etiqueta().trim(), tipoAttr, solicitud.obligatorio(),
                    opcionesValidas(tipoAttr, solicitud.opciones()),
                    valor(solicitud.orden(), existente.getOrden()));
            atributos.save(existente);
            return AtributoDelTipo.de(existente);
        }
        AtributoTipoRecurso a = AtributoTipoRecurso.nuevo(negocioId, tipoId, campo,
                solicitud.etiqueta().trim(), tipoAttr, solicitud.obligatorio(),
                opcionesValidas(tipoAttr, solicitud.opciones()), valor(solicitud.orden(), 0));
        atributos.save(a);
        return AtributoDelTipo.de(a);
    }

    @Transactional
    @RequierePermiso("RECURSOS_RECURSO_EDITAR")
    public void quitarAtributo(UUID atributoId) {
        AtributoTipoRecurso a = atributos
                .findByIdAndNegocioId(atributoId, ContextoDeNegocio.negocioActual())
                .orElseThrow(() -> new NoEncontradoException("Ese atributo no existe"));
        atributos.delete(a);
    }

    private List<AtributoDelTipo> atributosDe(UUID tipoId) {
        return atributos.findByTipoRecursoIdOrderByOrdenAscNombreCampoAsc(tipoId).stream()
                .map(AtributoDelTipo::de).toList();
    }

    private TipoDeRecurso delNegocio(UUID tipoId) {
        return tipos.findByIdAndNegocioId(tipoId, ContextoDeNegocio.negocioActual())
                .orElseThrow(() -> new NoEncontradoException("Ese tipo de recurso no existe"));
    }

    private static List<String> opcionesValidas(TipoAtributoRecurso tipo, List<String> opciones) {
        if (tipo.esDeOpciones() && (opciones == null || opciones.isEmpty())) {
            throw new ReglaDeNegocioException("Un atributo de lista necesita sus opciones");
        }
        return opciones;
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

    private static TipoAtributoRecurso tipoAtributoDe(String texto) {
        if (texto == null || texto.isBlank()) {
            return TipoAtributoRecurso.TEXTO;
        }
        try {
            return TipoAtributoRecurso.valueOf(texto.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException noExiste) {
            throw new ReglaDeNegocioException("Tipo de atributo no válido: " + texto);
        }
    }

    private static int valor(Integer v, int porDefecto) {
        return v == null ? porDefecto : v;
    }

    private static String limpiar(String v) {
        if (v == null) {
            return null;
        }
        String t = v.trim();
        return t.isEmpty() ? null : t;
    }
}
