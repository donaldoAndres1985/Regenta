package com.regenta.menu.application;

import com.regenta.menu.api.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.shared.web.NoEncontradoException;
import com.regenta.shared.web.RecursoDuplicadoException;
import com.regenta.shared.tenancy.ContextoDeNegocio;
import com.regenta.shared.security.RequierePermiso;
import com.regenta.menu.application.CartaDeMenu.CategoriaConItems;
import com.regenta.menu.domain.Carta;
import com.regenta.menu.domain.CategoriaMenu;
import com.regenta.menu.domain.CursoDeMenu;
import com.regenta.menu.domain.DisponibilidadDiaria;
import com.regenta.menu.domain.ItemDeMenu;
import com.regenta.menu.domain.TipoDeItem;
import com.regenta.menu.infrastructure.CartaRepositorio;
import com.regenta.menu.infrastructure.CategoriaMenuRepositorio;
import com.regenta.menu.infrastructure.DisponibilidadDiariaRepositorio;
import com.regenta.menu.infrastructure.EstacionDeCocinaRepositorio;
import com.regenta.menu.infrastructure.ItemDeMenuRepositorio;

/**
 * Ãtems de menÃº (HU-077). Cada uno con su categorÃ­a, precio, estaciÃ³n y tiempo
 * de preparaciÃ³n (criterio 1); el cÃ³digo es Ãºnico por negocio (criterio 2);
 * alÃ©rgenos, apto vegano y picante van al JSONB {@code atributos} (criterio 3).
 * Un Ã­tem no disponible sigue en la carta del mesero, pero "agotado" y sin poder
 * pedirlo (criterio 4).
 */
@Service
public class GestionDeItemsDeMenu {

    private final ItemDeMenuRepositorio items;
    private final CategoriaMenuRepositorio categorias;
    private final EstacionDeCocinaRepositorio estaciones;
    private final CartaRepositorio cartas;
    private final DisponibilidadDiariaRepositorio disponibilidad;

    public GestionDeItemsDeMenu(ItemDeMenuRepositorio items, CategoriaMenuRepositorio categorias,
            EstacionDeCocinaRepositorio estaciones, CartaRepositorio cartas,
            DisponibilidadDiariaRepositorio disponibilidad) {
        this.items = items;
        this.categorias = categorias;
        this.estaciones = estaciones;
        this.cartas = cartas;
        this.disponibilidad = disponibilidad;
    }

    @Transactional(readOnly = true)
    @RequierePermiso("MENU_PLATO_VER")
    public List<ItemDelNegocio> listarPorCategoria(UUID categoriaId) {
        categoriaDelNegocio(categoriaId);
        return items.findByCategoriaMenuIdAndEliminadoEnIsNullOrderByOrdenAscNombreAsc(categoriaId)
                .stream().map(ItemDelNegocio::de).toList();
    }

    @Transactional(readOnly = true)
    @RequierePermiso("MENU_PLATO_VER")
    public ItemDelNegocio ver(UUID itemId) {
        return ItemDelNegocio.de(delNegocio(itemId));
    }

    /** La carta como la ve el mesero: categorÃ­as en orden, cada una con sus Ã­tems (criterio 4). */
    @Transactional(readOnly = true)
    @RequierePermiso("MENU_PLATO_VER")
    public CartaDeMenu menuDeCarta(UUID cartaId) {
        Carta carta = cartas.findByIdAndNegocioId(cartaId, ContextoDeNegocio.negocioActual())
                .orElseThrow(() -> new NoEncontradoException("Esa carta no existe"));
        List<CategoriaMenu> cats = categorias.findByCartaIdOrderByOrdenAscNombreAsc(cartaId);
        List<ItemDeMenu> todos = cats.isEmpty() ? List.of()
                : items.findByCategoriaMenuIdInAndEliminadoEnIsNullOrderByOrdenAscNombreAsc(
                        cats.stream().map(CategoriaMenu::getId).toList());
        Map<UUID, List<ItemDeMenu>> porCategoria = todos.stream()
                .collect(Collectors.groupingBy(ItemDeMenu::getCategoriaMenuId));
        Set<UUID> agotadosHoy = todos.isEmpty() ? Set.of()
                : disponibilidad.findByFechaAndItemIdIn(LocalDate.now(),
                                todos.stream().map(ItemDeMenu::getId).toList())
                        .stream().filter(DisponibilidadDiaria::estaAgotado)
                        .map(DisponibilidadDiaria::getItemId).collect(Collectors.toSet());

        List<CategoriaConItems> bloques = cats.stream()
                .map(c -> new CategoriaConItems(CategoriaDelNegocio.de(c),
                        porCategoria.getOrDefault(c.getId(), List.of()).stream()
                                .map(i -> ItemEnCarta.de(i, agotadosHoy.contains(i.getId())))
                                .toList()))
                .toList();
        return new CartaDeMenu(CartaDelNegocio.de(carta), bloques);
    }

    @Transactional
    @RequierePermiso("MENU_PLATO_CREAR")
    public ItemDelNegocio crear(SolicitudDeItem solicitud) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        categoriaDelNegocio(solicitud.categoriaMenuId());
        exigirEstacion(solicitud.estacionId(), negocioId);

        String codigo = solicitud.codigo().trim();
        if (items.existsByNegocioIdAndCodigo(negocioId, codigo)) {
            throw new RecursoDuplicadoException("Ya hay un Ã­tem con el cÃ³digo " + codigo);
        }
        ItemDeMenu item = ItemDeMenu.crear(negocioId, solicitud.categoriaMenuId(),
                solicitud.estacionId(), codigo, solicitud.nombre().trim(), solicitud.descripcion(),
                TipoDeItem.desde(solicitud.tipo()), solicitud.precio(), solicitud.impuestoId(),
                solicitud.precioIncluyeImpuesto() == null || solicitud.precioIncluyeImpuesto(),
                corto(solicitud.tiempoPreparacionMin()), cursoDe(solicitud.curso()),
                solicitud.atributos(), solicitud.imagenUrl(),
                solicitud.orden() == null ? 0 : solicitud.orden());
        try {
            items.save(item);
        } catch (DataIntegrityViolationException choca) {
            throw new RecursoDuplicadoException("Ya hay un Ã­tem con el cÃ³digo " + codigo);
        }
        return ItemDelNegocio.de(item);
    }

    @Transactional
    @RequierePermiso("MENU_PLATO_EDITAR")
    public ItemDelNegocio actualizar(UUID itemId, SolicitudDeItem solicitud) {
        ItemDeMenu item = delNegocio(itemId);
        categoriaDelNegocio(solicitud.categoriaMenuId());
        exigirEstacion(solicitud.estacionId(), item.getNegocioId());
        item.editar(solicitud.estacionId(), solicitud.nombre().trim(), solicitud.descripcion(),
                TipoDeItem.desde(solicitud.tipo()), solicitud.precio(), solicitud.impuestoId(),
                solicitud.precioIncluyeImpuesto() == null || solicitud.precioIncluyeImpuesto(),
                corto(solicitud.tiempoPreparacionMin()), cursoDe(solicitud.curso()),
                solicitud.atributos(), solicitud.imagenUrl(),
                solicitud.orden() == null ? item.getOrden() : solicitud.orden());
        items.save(item);
        return ItemDelNegocio.de(item);
    }

    @Transactional
    @RequierePermiso("MENU_PLATO_EDITAR")
    public ItemDelNegocio cambiarDisponibilidad(UUID itemId, boolean disponible) {
        ItemDeMenu item = delNegocio(itemId);
        item.marcarDisponible(disponible);
        items.save(item);
        return ItemDelNegocio.de(item);
    }

    @Transactional
    @RequierePermiso("MENU_PLATO_ELIMINAR")
    public void eliminar(UUID itemId) {
        ItemDeMenu item = delNegocio(itemId);
        item.eliminar(OffsetDateTime.now());
        items.save(item);
    }

    private void exigirEstacion(UUID estacionId, UUID negocioId) {
        if (estacionId != null
                && estaciones.findByIdAndNegocioId(estacionId, negocioId).isEmpty()) {
            throw new NoEncontradoException("Esa estaciÃ³n de cocina no existe");
        }
    }

    private CategoriaMenu categoriaDelNegocio(UUID categoriaId) {
        return categorias.findByIdAndNegocioId(categoriaId, ContextoDeNegocio.negocioActual())
                .orElseThrow(() -> new NoEncontradoException("Esa categorÃ­a no existe"));
    }

    private ItemDeMenu delNegocio(UUID itemId) {
        return items.findByIdAndNegocioId(itemId, ContextoDeNegocio.negocioActual())
                .filter(i -> !i.estaEliminado())
                .orElseThrow(() -> new NoEncontradoException("Ese Ã­tem no existe"));
    }

    private static CursoDeMenu cursoDe(String texto) {
        return texto == null || texto.isBlank() ? null : CursoDeMenu.desde(texto);
    }

    private static Short corto(Integer v) {
        return v == null ? null : v.shortValue();
    }
}
