package com.regenta.menu.aplicacion;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.comun.errores.ConflictoDeEstadoException;
import com.regenta.comun.errores.NoEncontradoException;
import com.regenta.comun.negocio.ContextoDeNegocio;
import com.regenta.comun.negocio.RequierePermiso;
import com.regenta.menu.domain.Carta;
import com.regenta.menu.domain.CategoriaMenu;
import com.regenta.menu.infra.CartaRepositorio;
import com.regenta.menu.infra.CategoriaMenuRepositorio;
import com.regenta.menu.infra.ConteoDeItemsDeCarta;

/**
 * Cartas y categorías del menú (HU-076). Una carta vale para una franja horaria,
 * unos días y un rango de fechas; fuera de eso no se ofrece (criterio 1). Sus
 * categorías se ven en el orden definido (criterio 2). Una carta con platos no
 * se puede borrar (criterio 3).
 */
@Service
public class GestionDeCartas {

    private final CartaRepositorio cartas;
    private final CategoriaMenuRepositorio categorias;
    private final ConteoDeItemsDeCarta conteo;

    public GestionDeCartas(CartaRepositorio cartas, CategoriaMenuRepositorio categorias,
            ConteoDeItemsDeCarta conteo) {
        this.cartas = cartas;
        this.categorias = categorias;
        this.conteo = conteo;
    }

    @Transactional(readOnly = true)
    @RequierePermiso("MENU_PLATO_VER")
    public List<CartaDelNegocio> listar() {
        return cartas.findByNegocioIdOrderByEsDefaultDescNombreAsc(ContextoDeNegocio.negocioActual())
                .stream().map(CartaDelNegocio::de).toList();
    }

    /** Las cartas activas que valen en ese instante (HU-076 criterio 1). */
    @Transactional(readOnly = true)
    @RequierePermiso("MENU_PLATO_VER")
    public List<CartaDelNegocio> disponibles(OffsetDateTime momento) {
        LocalDateTime cuando = (momento == null ? OffsetDateTime.now() : momento)
                .toLocalDateTime();
        return cartas.findByNegocioIdAndActivaTrueOrderByEsDefaultDescNombreAsc(
                        ContextoDeNegocio.negocioActual())
                .stream().filter(c -> c.disponibleEn(cuando)).map(CartaDelNegocio::de).toList();
    }

    @Transactional(readOnly = true)
    @RequierePermiso("MENU_PLATO_VER")
    public CartaConCategorias ver(UUID cartaId) {
        Carta carta = delNegocio(cartaId);
        List<CategoriaDelNegocio> lista = categorias.findByCartaIdOrderByOrdenAscNombreAsc(cartaId)
                .stream().map(CategoriaDelNegocio::de).toList();
        return new CartaConCategorias(CartaDelNegocio.de(carta), lista);
    }

    @Transactional
    @RequierePermiso("MENU_PLATO_CREAR")
    public CartaDelNegocio crear(SolicitudDeCarta solicitud) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        Carta carta = Carta.crear(negocioId, solicitud.sucursalId(), solicitud.nombre().trim(),
                solicitud.descripcion(), solicitud.horaDesde(), solicitud.horaHasta(),
                solicitud.diasSemana(), solicitud.vigenteDesde(), solicitud.vigenteHasta(),
                solicitud.esDefault());
        cartas.save(carta);
        if (carta.isEsDefault()) {
            cartas.quitarDefaultSalvo(negocioId, carta.getId());
        }
        return CartaDelNegocio.de(carta);
    }

    @Transactional
    @RequierePermiso("MENU_PLATO_EDITAR")
    public CartaDelNegocio actualizar(UUID cartaId, SolicitudDeCarta solicitud) {
        Carta carta = delNegocio(cartaId);
        carta.editar(solicitud.nombre().trim(), solicitud.descripcion(), solicitud.horaDesde(),
                solicitud.horaHasta(), solicitud.diasSemana(), solicitud.vigenteDesde(),
                solicitud.vigenteHasta());
        if (solicitud.esDefault()) {
            carta.marcarPorDefecto();
        }
        cartas.save(carta);
        if (carta.isEsDefault()) {
            cartas.quitarDefaultSalvo(carta.getNegocioId(), carta.getId());
        }
        return CartaDelNegocio.de(carta);
    }

    @Transactional
    @RequierePermiso("MENU_PLATO_EDITAR")
    public CartaDelNegocio cambiarActivacion(UUID cartaId, boolean activa) {
        Carta carta = delNegocio(cartaId);
        if (activa) {
            carta.activar();
        } else {
            carta.desactivar();
        }
        cartas.save(carta);
        return CartaDelNegocio.de(carta);
    }

    @Transactional
    @RequierePermiso("MENU_PLATO_EDITAR")
    public CartaDelNegocio marcarPorDefecto(UUID cartaId) {
        Carta carta = delNegocio(cartaId);
        carta.marcarPorDefecto();
        cartas.save(carta);
        cartas.quitarDefaultSalvo(carta.getNegocioId(), carta.getId());
        return CartaDelNegocio.de(carta);
    }

    @Transactional
    @RequierePermiso("MENU_PLATO_ELIMINAR")
    public void eliminar(UUID cartaId) {
        Carta carta = delNegocio(cartaId);
        if (conteo.itemsDe(cartaId) > 0) {
            throw new ConflictoDeEstadoException(
                    "La carta tiene platos; quitalos o pasalos a otra carta antes de borrarla");
        }
        cartas.delete(carta);
    }

    @Transactional
    @RequierePermiso("MENU_PLATO_EDITAR")
    public CategoriaDelNegocio crearCategoria(UUID cartaId, SolicitudDeCategoria solicitud) {
        Carta carta = delNegocio(cartaId);
        CategoriaMenu categoria = CategoriaMenu.crear(carta.getNegocioId(), cartaId,
                solicitud.nombre().trim(), solicitud.descripcion(),
                solicitud.orden() == null ? proximoOrden(cartaId) : solicitud.orden(),
                solicitud.icono());
        categorias.save(categoria);
        return CategoriaDelNegocio.de(categoria);
    }

    @Transactional
    @RequierePermiso("MENU_PLATO_EDITAR")
    public CategoriaDelNegocio actualizarCategoria(UUID categoriaId, SolicitudDeCategoria solicitud) {
        CategoriaMenu categoria = categoriaDelNegocio(categoriaId);
        categoria.editar(solicitud.nombre().trim(), solicitud.descripcion(),
                solicitud.orden() == null ? categoria.getOrden() : solicitud.orden(),
                solicitud.icono());
        categorias.save(categoria);
        return CategoriaDelNegocio.de(categoria);
    }

    @Transactional
    @RequierePermiso("MENU_PLATO_EDITAR")
    public void quitarCategoria(UUID categoriaId) {
        categorias.delete(categoriaDelNegocio(categoriaId));
    }

    /** Fija el orden de las categorías de una carta según la lista recibida (HU-076 criterio 2). */
    @Transactional
    @RequierePermiso("MENU_PLATO_EDITAR")
    public List<CategoriaDelNegocio> reordenarCategorias(UUID cartaId, List<UUID> ordenIds) {
        delNegocio(cartaId);
        List<CategoriaMenu> deLaCarta = categorias.findByCartaIdOrderByOrdenAscNombreAsc(cartaId);
        for (CategoriaMenu c : deLaCarta) {
            int pos = ordenIds == null ? -1 : ordenIds.indexOf(c.getId());
            if (pos >= 0) {
                c.reordenar(pos);
            }
        }
        categorias.saveAll(deLaCarta);
        return categorias.findByCartaIdOrderByOrdenAscNombreAsc(cartaId).stream()
                .map(CategoriaDelNegocio::de).toList();
    }

    private int proximoOrden(UUID cartaId) {
        return categorias.findByCartaIdOrderByOrdenAscNombreAsc(cartaId).stream()
                .mapToInt(CategoriaMenu::getOrden).max().orElse(-1) + 1;
    }

    private Carta delNegocio(UUID cartaId) {
        return cartas.findByIdAndNegocioId(cartaId, ContextoDeNegocio.negocioActual())
                .orElseThrow(() -> new NoEncontradoException("Esa carta no existe"));
    }

    private CategoriaMenu categoriaDelNegocio(UUID categoriaId) {
        return categorias.findByIdAndNegocioId(categoriaId, ContextoDeNegocio.negocioActual())
                .orElseThrow(() -> new NoEncontradoException("Esa categoría no existe"));
    }
}
