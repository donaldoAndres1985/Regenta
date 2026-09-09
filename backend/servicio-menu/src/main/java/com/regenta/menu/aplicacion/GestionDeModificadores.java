package com.regenta.menu.aplicacion;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.comun.errores.NoEncontradoException;
import com.regenta.comun.errores.ReglaDeNegocioException;
import com.regenta.comun.negocio.ContextoDeNegocio;
import com.regenta.comun.negocio.RequierePermiso;
import com.regenta.menu.aplicacion.CotizacionDeModificadores.ModificadorElegido;
import com.regenta.menu.domain.GrupoDeModificadores;
import com.regenta.menu.domain.ItemGrupoModificador;
import com.regenta.menu.domain.Modificador;
import com.regenta.menu.infra.GrupoDeModificadoresRepositorio;
import com.regenta.menu.infra.ItemDeMenuRepositorio;
import com.regenta.menu.infra.ItemGrupoModificadorRepositorio;
import com.regenta.menu.infra.ModificadorRepositorio;

/**
 * Grupos de modificadores, sus opciones y qué grupos aplican a cada ítem
 * (HU-078). {@link #cotizarSeleccion} valida una selección contra los mínimos y
 * máximos de los grupos (criterios 1 y 2) y suma los precios extra (criterio 3);
 * la base rechaza un grupo con {@code max < min} (criterio 4).
 */
@Service
public class GestionDeModificadores {

    private final GrupoDeModificadoresRepositorio grupos;
    private final ModificadorRepositorio modificadores;
    private final ItemGrupoModificadorRepositorio vinculos;
    private final ItemDeMenuRepositorio items;

    public GestionDeModificadores(GrupoDeModificadoresRepositorio grupos,
            ModificadorRepositorio modificadores, ItemGrupoModificadorRepositorio vinculos,
            ItemDeMenuRepositorio items) {
        this.grupos = grupos;
        this.modificadores = modificadores;
        this.vinculos = vinculos;
        this.items = items;
    }

    // ---- grupos --------------------------------------------------------------

    @Transactional(readOnly = true)
    @RequierePermiso("MENU_PLATO_VER")
    public List<GrupoConOpciones> listarGrupos() {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        return grupos.findByNegocioIdOrderByNombreAsc(negocioId).stream()
                .map(g -> new GrupoConOpciones(GrupoDelNegocio.de(g), 0,
                        modificadores.findByGrupoIdOrderByOrdenAscNombreAsc(g.getId()).stream()
                                .map(ModificadorDelNegocio::de).toList()))
                .toList();
    }

    @Transactional(readOnly = true)
    @RequierePermiso("MENU_PLATO_VER")
    public GrupoConOpciones verGrupo(UUID grupoId) {
        GrupoDeModificadores g = grupoDelNegocio(grupoId);
        return new GrupoConOpciones(GrupoDelNegocio.de(g), 0,
                modificadores.findByGrupoIdOrderByOrdenAscNombreAsc(grupoId).stream()
                        .map(ModificadorDelNegocio::de).toList());
    }

    @Transactional
    @RequierePermiso("MENU_PLATO_CREAR")
    public GrupoDelNegocio crearGrupo(SolicitudDeGrupo solicitud) {
        GrupoDeModificadores g = GrupoDeModificadores.crear(ContextoDeNegocio.negocioActual(),
                solicitud.nombre().trim(), valor(solicitud.minSelecciones(), 0),
                valor(solicitud.maxSelecciones(), 1));
        try {
            grupos.save(g);
        } catch (DataIntegrityViolationException choca) {
            throw new ReglaDeNegocioException(
                    "El máximo de selecciones no puede ser menor que el mínimo");
        }
        return GrupoDelNegocio.de(g);
    }

    @Transactional
    @RequierePermiso("MENU_PLATO_EDITAR")
    public GrupoDelNegocio actualizarGrupo(UUID grupoId, SolicitudDeGrupo solicitud) {
        GrupoDeModificadores g = grupoDelNegocio(grupoId);
        g.editar(solicitud.nombre().trim(), valor(solicitud.minSelecciones(), g.getMinSelecciones()),
                valor(solicitud.maxSelecciones(), g.getMaxSelecciones()));
        grupos.save(g);
        return GrupoDelNegocio.de(g);
    }

    @Transactional
    @RequierePermiso("MENU_PLATO_EDITAR")
    public GrupoDelNegocio cambiarActivacionGrupo(UUID grupoId, boolean activo) {
        GrupoDeModificadores g = grupoDelNegocio(grupoId);
        if (activo) {
            g.activar();
        } else {
            g.desactivar();
        }
        grupos.save(g);
        return GrupoDelNegocio.de(g);
    }

    @Transactional
    @RequierePermiso("MENU_PLATO_ELIMINAR")
    public void eliminarGrupo(UUID grupoId) {
        grupos.delete(grupoDelNegocio(grupoId));
    }

    // ---- modificadores -----------------------------------------------------

    @Transactional
    @RequierePermiso("MENU_PLATO_EDITAR")
    public ModificadorDelNegocio agregarModificador(UUID grupoId, SolicitudDeModificador solicitud) {
        GrupoDeModificadores g = grupoDelNegocio(grupoId);
        Modificador m = Modificador.crear(g.getNegocioId(), grupoId, solicitud.nombre().trim(),
                solicitud.precioExtra(), solicitud.productoId(), solicitud.cantidadInsumo(),
                valor(solicitud.orden(), 0));
        modificadores.save(m);
        return ModificadorDelNegocio.de(m);
    }

    @Transactional
    @RequierePermiso("MENU_PLATO_EDITAR")
    public ModificadorDelNegocio actualizarModificador(UUID modificadorId,
            SolicitudDeModificador solicitud) {
        Modificador m = modificadorDelNegocio(modificadorId);
        m.editar(solicitud.nombre().trim(), solicitud.precioExtra(), solicitud.productoId(),
                solicitud.cantidadInsumo(), valor(solicitud.orden(), m.getOrden()));
        modificadores.save(m);
        return ModificadorDelNegocio.de(m);
    }

    @Transactional
    @RequierePermiso("MENU_PLATO_EDITAR")
    public ModificadorDelNegocio cambiarActivacionModificador(UUID modificadorId, boolean activo) {
        Modificador m = modificadorDelNegocio(modificadorId);
        if (activo) {
            m.activar();
        } else {
            m.desactivar();
        }
        modificadores.save(m);
        return ModificadorDelNegocio.de(m);
    }

    @Transactional
    @RequierePermiso("MENU_PLATO_EDITAR")
    public void quitarModificador(UUID modificadorId) {
        modificadores.delete(modificadorDelNegocio(modificadorId));
    }

    // ---- vínculo ítem <-> grupo -----------------------------------------------

    @Transactional
    @RequierePermiso("MENU_PLATO_EDITAR")
    public List<GrupoConOpciones> vincular(UUID itemId, UUID grupoId, Integer orden) {
        exigirItem(itemId);
        grupoDelNegocio(grupoId);
        if (!vinculos.existsByItemIdAndGrupoId(itemId, grupoId)) {
            vinculos.save(ItemGrupoModificador.de(ContextoDeNegocio.negocioActual(), itemId,
                    grupoId, valor(orden, 0)));
        }
        return gruposDeItem(itemId);
    }

    @Transactional
    @RequierePermiso("MENU_PLATO_EDITAR")
    public void desvincular(UUID itemId, UUID grupoId) {
        exigirItem(itemId);
        vinculos.deleteByItemIdAndGrupoId(itemId, grupoId);
    }

    @Transactional(readOnly = true)
    @RequierePermiso("MENU_PLATO_VER")
    public List<GrupoConOpciones> gruposDeItem(UUID itemId) {
        exigirItem(itemId);
        List<ItemGrupoModificador> links = vinculos.findByItemIdOrderByOrdenAsc(itemId);
        if (links.isEmpty()) {
            return List.of();
        }
        Map<UUID, GrupoDeModificadores> porId = grupos
                .findByIdIn(links.stream().map(ItemGrupoModificador::getGrupoId).toList())
                .stream().collect(Collectors.toMap(GrupoDeModificadores::getId, g -> g));
        Map<UUID, List<Modificador>> opcionesPorGrupo = modificadores
                .findByGrupoIdInOrderByOrdenAscNombreAsc(porId.keySet()).stream()
                .collect(Collectors.groupingBy(Modificador::getGrupoId));
        return links.stream()
                .filter(l -> porId.containsKey(l.getGrupoId()))
                .map(l -> new GrupoConOpciones(GrupoDelNegocio.de(porId.get(l.getGrupoId())),
                        l.getOrden(),
                        opcionesPorGrupo.getOrDefault(l.getGrupoId(), List.of()).stream()
                                .map(ModificadorDelNegocio::de).toList()))
                .toList();
    }

    // ---- validación de una selección ---------------------------------------

    /**
     * Valida una selección de modificadores para un ítem y devuelve cuánto suma
     * al total de la línea (HU-078). Lo consumirá el servicio de comandas.
     */
    @Transactional(readOnly = true)
    @RequierePermiso("MENU_PLATO_VER")
    public CotizacionDeModificadores cotizarSeleccion(UUID itemId, List<UUID> modificadorIds) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        exigirItem(itemId);

        List<ItemGrupoModificador> links = vinculos.findByItemIdOrderByOrdenAsc(itemId);
        Map<UUID, GrupoDeModificadores> gruposDelItem = links.isEmpty() ? Map.of()
                : grupos.findByIdIn(links.stream().map(ItemGrupoModificador::getGrupoId).toList())
                        .stream().collect(Collectors.toMap(GrupoDeModificadores::getId, g -> g));

        List<UUID> ids = modificadorIds == null ? List.of() : modificadorIds;
        List<Modificador> elegidos = ids.isEmpty() ? List.of()
                : modificadores.findByIdInAndNegocioId(ids, negocioId);
        if (elegidos.size() != ids.stream().distinct().count()) {
            throw new NoEncontradoException("Alguno de los modificadores elegidos no existe");
        }
        for (Modificador m : elegidos) {
            if (!gruposDelItem.containsKey(m.getGrupoId()) || !m.isActivo()) {
                throw new ReglaDeNegocioException(
                        "El modificador \"" + m.getNombre() + "\" no aplica a este ítem");
            }
        }

        Map<UUID, Long> cuantosPorGrupo = elegidos.stream()
                .collect(Collectors.groupingBy(Modificador::getGrupoId, Collectors.counting()));
        for (GrupoDeModificadores g : gruposDelItem.values()) {
            int cuantos = cuantosPorGrupo.getOrDefault(g.getId(), 0L).intValue();
            if (cuantos < g.getMinSelecciones()) {
                throw new ReglaDeNegocioException("El grupo \"" + g.getNombre()
                        + "\" exige elegir al menos " + g.getMinSelecciones());
            }
            if (cuantos > g.getMaxSelecciones()) {
                throw new ReglaDeNegocioException("El grupo \"" + g.getNombre()
                        + "\" admite a lo sumo " + g.getMaxSelecciones());
            }
        }

        List<ModificadorElegido> detalle = new java.util.ArrayList<>();
        BigDecimal extra = BigDecimal.ZERO;
        for (Modificador m : elegidos) {
            extra = extra.add(m.getPrecioExtra());
            GrupoDeModificadores g = gruposDelItem.get(m.getGrupoId());
            detalle.add(new ModificadorElegido(m.getId(), m.getGrupoId(),
                    g == null ? null : g.getNombre(), m.getNombre(), m.getPrecioExtra()));
        }
        return new CotizacionDeModificadores(detalle,
                extra.setScale(4, java.math.RoundingMode.HALF_UP));
    }

    // ---- helpers ----------------------------------------------------------

    private static int valor(Integer v, int porDefecto) {
        return v == null ? porDefecto : v;
    }

    private void exigirItem(UUID itemId) {
        items.findByIdAndNegocioId(itemId, ContextoDeNegocio.negocioActual())
                .filter(i -> !i.estaEliminado())
                .orElseThrow(() -> new NoEncontradoException("Ese ítem no existe"));
    }

    private GrupoDeModificadores grupoDelNegocio(UUID grupoId) {
        return grupos.findByIdAndNegocioId(grupoId, ContextoDeNegocio.negocioActual())
                .orElseThrow(() -> new NoEncontradoException("Ese grupo no existe"));
    }

    private Modificador modificadorDelNegocio(UUID modificadorId) {
        return modificadores.findByIdAndNegocioId(modificadorId, ContextoDeNegocio.negocioActual())
                .orElseThrow(() -> new NoEncontradoException("Ese modificador no existe"));
    }
}
