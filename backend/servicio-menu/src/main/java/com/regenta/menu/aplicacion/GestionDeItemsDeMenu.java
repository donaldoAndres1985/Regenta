package com.regenta.menu.aplicacion;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.comun.errores.NoEncontradoException;
import com.regenta.comun.errores.RecursoDuplicadoException;
import com.regenta.comun.negocio.ContextoDeNegocio;
import com.regenta.comun.negocio.RequierePermiso;
import com.regenta.menu.aplicacion.CartaDeMenu.CategoriaConItems;
import com.regenta.menu.domain.Carta;
import com.regenta.menu.domain.CategoriaMenu;
import com.regenta.menu.domain.CursoDeMenu;
import com.regenta.menu.domain.ItemDeMenu;
import com.regenta.menu.domain.TipoDeItem;
import com.regenta.menu.infra.CartaRepositorio;
import com.regenta.menu.infra.CategoriaMenuRepositorio;
import com.regenta.menu.infra.EstacionDeCocinaRepositorio;
import com.regenta.menu.infra.ItemDeMenuRepositorio;

/**
 * Ítems de menú (HU-077). Cada uno con su categoría, precio, estación y tiempo
 * de preparación (criterio 1); el código es único por negocio (criterio 2);
 * alérgenos, apto vegano y picante van al JSONB {@code atributos} (criterio 3).
 * Un ítem no disponible sigue en la carta del mesero, pero "agotado" y sin poder
 * pedirlo (criterio 4).
 */
@Service
public class GestionDeItemsDeMenu {

    private final ItemDeMenuRepositorio items;
    private final CategoriaMenuRepositorio categorias;
    private final EstacionDeCocinaRepositorio estaciones;
    private final CartaRepositorio cartas;

    public GestionDeItemsDeMenu(ItemDeMenuRepositorio items, CategoriaMenuRepositorio categorias,
            EstacionDeCocinaRepositorio estaciones, CartaRepositorio cartas) {
        this.items = items;
        this.categorias = categorias;
        this.estaciones = estaciones;
        this.cartas = cartas;
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

    /** La carta como la ve el mesero: categorías en orden, cada una con sus ítems (criterio 4). */
    @Transactional(readOnly = true)
    @RequierePermiso("MENU_PLATO_VER")
    public CartaDeMenu menuDeCarta(UUID cartaId) {
        Carta carta = cartas.findByIdAndNegocioId(cartaId, ContextoDeNegocio.negocioActual())
                .orElseThrow(() -> new NoEncontradoException("Esa carta no existe"));
        List<CategoriaMenu> cats = categorias.findByCartaIdOrderByOrdenAscNombreAsc(cartaId);
        Map<UUID, List<ItemDeMenu>> porCategoria = cats.isEmpty() ? Map.of()
                : items.findByCategoriaMenuIdInAndEliminadoEnIsNullOrderByOrdenAscNombreAsc(
                                cats.stream().map(CategoriaMenu::getId).toList())
                        .stream().collect(Collectors.groupingBy(ItemDeMenu::getCategoriaMenuId));

        List<CategoriaConItems> bloques = cats.stream()
                .map(c -> new CategoriaConItems(CategoriaDelNegocio.de(c),
                        porCategoria.getOrDefault(c.getId(), List.of()).stream()
                                .map(ItemEnCarta::de).toList()))
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
            throw new RecursoDuplicadoException("Ya hay un ítem con el código " + codigo);
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
            throw new RecursoDuplicadoException("Ya hay un ítem con el código " + codigo);
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
            throw new NoEncontradoException("Esa estación de cocina no existe");
        }
    }

    private CategoriaMenu categoriaDelNegocio(UUID categoriaId) {
        return categorias.findByIdAndNegocioId(categoriaId, ContextoDeNegocio.negocioActual())
                .orElseThrow(() -> new NoEncontradoException("Esa categoría no existe"));
    }

    private ItemDeMenu delNegocio(UUID itemId) {
        return items.findByIdAndNegocioId(itemId, ContextoDeNegocio.negocioActual())
                .filter(i -> !i.estaEliminado())
                .orElseThrow(() -> new NoEncontradoException("Ese ítem no existe"));
    }

    private static CursoDeMenu cursoDe(String texto) {
        return texto == null || texto.isBlank() ? null : CursoDeMenu.desde(texto);
    }

    private static Short corto(Integer v) {
        return v == null ? null : v.shortValue();
    }
}
