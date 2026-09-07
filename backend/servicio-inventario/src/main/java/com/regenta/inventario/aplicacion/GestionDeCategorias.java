package com.regenta.inventario.aplicacion;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.comun.errores.ConflictoDeEstadoException;
import com.regenta.comun.errores.NoEncontradoException;
import com.regenta.comun.errores.RecursoDuplicadoException;
import com.regenta.comun.eventos.RegistroDeEventos;
import com.regenta.comun.negocio.ContextoDeNegocio;
import com.regenta.comun.negocio.RequierePermiso;
import com.regenta.inventario.domain.AtributoCategoria;
import com.regenta.inventario.domain.Categoria;
import com.regenta.inventario.infra.AtributoCategoriaRepositorio;
import com.regenta.inventario.infra.CategoriaRepositorio;

/**
 * Categorias del negocio, con jerarquia. HU-026.
 *
 * <p>Nunca vienen precargadas: cada negocio crea las suyas, con sus palabras.
 * Una subcategoria hereda del padre los atributos marcados heredables (criterio
 * 4): asi "Medicamentos" define lote y vencimiento una vez y "Medicamentos /
 * Controlados" ya los pide.
 */
@Service
public class GestionDeCategorias {

    private final CategoriaRepositorio categorias;
    private final AtributoCategoriaRepositorio atributos;
    private final RegistroDeEventos eventos;

    public GestionDeCategorias(CategoriaRepositorio categorias,
            AtributoCategoriaRepositorio atributos, RegistroDeEventos eventos) {
        this.categorias = categorias;
        this.atributos = atributos;
        this.eventos = eventos;
    }

    @Transactional(readOnly = true)
    @RequierePermiso("INVENTARIO_CATEGORIA_VER")
    public List<CategoriaDelNegocio> listar() {
        return categorias
                .findByNegocioIdOrderByNivelAscOrdenAscNombreAsc(ContextoDeNegocio.negocioActual())
                .stream()
                .map(GestionDeCategorias::comoDto)
                .toList();
    }

    @Transactional
    @RequierePermiso("INVENTARIO_CATEGORIA_CREAR")
    public CategoriaDelNegocio crear(SolicitudDeCategoria solicitud) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        Categoria padre = padreDe(solicitud.categoriaPadreId(), negocioId);
        comprobarNombreLibre(negocioId, solicitud.categoriaPadreId(), solicitud.nombre());

        Categoria categoria = padre == null
                ? Categoria.raiz(negocioId, solicitud.nombre())
                : Categoria.hija(negocioId, padre, solicitud.nombre());
        categoria.describir(solicitud.descripcion(), solicitud.icono(), solicitud.color(),
                solicitud.orden());
        categorias.save(categoria);

        if (padre != null) {
            heredarAtributos(padre.getId(), categoria.getId());
        }

        eventos.registrar(negocioId, "categoria", categoria.getId(), "categoria_creada",
                datos(negocioId, categoria));
        return comoDto(categoria);
    }

    @Transactional
    @RequierePermiso("INVENTARIO_CATEGORIA_EDITAR")
    public CategoriaDelNegocio actualizar(UUID categoriaId, SolicitudDeCategoria solicitud) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        Categoria categoria = buscar(categoriaId);
        if (!categoria.getNombre().equalsIgnoreCase(solicitud.nombre())) {
            comprobarNombreLibre(negocioId, categoria.getCategoriaPadreId(), solicitud.nombre());
            categoria.renombrar(solicitud.nombre());
        }
        categoria.describir(solicitud.descripcion(), solicitud.icono(), solicitud.color(),
                solicitud.orden());
        categorias.save(categoria);
        return comoDto(categoria);
    }

    /** Criterio 3: con productos dentro, no se elimina; primero se mueven. */
    @Transactional
    @RequierePermiso("INVENTARIO_CATEGORIA_ELIMINAR")
    public void eliminar(UUID categoriaId) {
        Categoria categoria = buscar(categoriaId);

        long productos = categorias.contarProductos(categoriaId);
        if (productos > 0) {
            throw new ConflictoDeEstadoException("La categoria \"" + categoria.getNombre()
                    + "\" tiene " + productos + " productos. Muevelos a otra categoria antes de"
                    + " eliminarla");
        }
        boolean tieneHijas = categorias
                .findByNegocioIdOrderByNivelAscOrdenAscNombreAsc(categoria.getNegocioId()).stream()
                .anyMatch(c -> categoriaId.equals(c.getCategoriaPadreId()));
        if (tieneHijas) {
            throw new ConflictoDeEstadoException("La categoria \"" + categoria.getNombre()
                    + "\" tiene subcategorias. Eliminalas o muevelas primero");
        }
        categorias.delete(categoria);
    }

    private Categoria padreDe(UUID padreId, UUID negocioId) {
        if (padreId == null) {
            return null;
        }
        Categoria padre = categorias.findById(padreId)
                .orElseThrow(() -> new NoEncontradoException("La categoria padre no existe"));
        if (!padre.getNegocioId().equals(negocioId)) {
            throw new NoEncontradoException("La categoria padre no existe");
        }
        return padre;
    }

    private void comprobarNombreLibre(UUID negocioId, UUID padreId, String nombre) {
        boolean repetido = padreId == null
                ? categorias.existsByNegocioIdAndCategoriaPadreIdIsNullAndNombreIgnoreCase(
                        negocioId, nombre)
                : categorias.existsByNegocioIdAndCategoriaPadreIdAndNombreIgnoreCase(
                        negocioId, padreId, nombre);
        if (repetido) {
            throw new RecursoDuplicadoException(
                    "Ya hay una categoria \"" + nombre + "\" con ese mismo padre");
        }
    }

    private void heredarAtributos(UUID padreId, UUID subcategoriaId) {
        List<AtributoCategoria> heredables =
                atributos.findByCategoriaIdAndHeredableTrueOrderByOrdenAsc(padreId);
        heredables.stream()
                .map(atributo -> atributo.heredarEn(subcategoriaId))
                .forEach(atributos::save);
    }

    private Categoria buscar(UUID categoriaId) {
        return categorias.findById(categoriaId)
                .orElseThrow(() -> new NoEncontradoException("Esa categoria no existe"));
    }

    private static Map<String, Object> datos(UUID negocioId, Categoria categoria) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("negocio_id", negocioId.toString());
        payload.put("categoria_id", categoria.getId().toString());
        payload.put("nombre", categoria.getNombre());
        payload.put("ruta", categoria.getRuta());
        payload.put("categoria_padre_id",
                categoria.getCategoriaPadreId() == null ? null
                        : categoria.getCategoriaPadreId().toString());
        return payload;
    }

    private static CategoriaDelNegocio comoDto(Categoria c) {
        return new CategoriaDelNegocio(c.getId(), c.getCategoriaPadreId(), c.getNombre(),
                c.getDescripcion(), c.getRuta(), c.getNivel(), c.getIcono(), c.getColor(),
                c.getOrden(), c.isActiva());
    }
}
