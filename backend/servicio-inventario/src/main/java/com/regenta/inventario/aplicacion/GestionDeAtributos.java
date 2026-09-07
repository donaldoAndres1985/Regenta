package com.regenta.inventario.aplicacion;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.comun.errores.NoEncontradoException;
import com.regenta.comun.errores.RecursoDuplicadoException;
import com.regenta.comun.errores.ReglaDeNegocioException;
import com.regenta.comun.negocio.ContextoDeNegocio;
import com.regenta.comun.negocio.RequierePermiso;
import com.regenta.inventario.domain.AtributoCategoria;
import com.regenta.inventario.domain.Categoria;
import com.regenta.inventario.infra.AtributoCategoriaRepositorio;
import com.regenta.inventario.infra.CategoriaRepositorio;

/**
 * Los campos que exige cada categoria. HU-027: la historia que sostiene la
 * tesis del producto. Ferreteria, papeleria y drogueria usan las mismas tablas;
 * lo unico que cambia son estas filas.
 */
@Service
public class GestionDeAtributos {

    private final AtributoCategoriaRepositorio atributos;
    private final CategoriaRepositorio categorias;

    public GestionDeAtributos(AtributoCategoriaRepositorio atributos,
            CategoriaRepositorio categorias) {
        this.atributos = atributos;
        this.categorias = categorias;
    }

    @Transactional(readOnly = true)
    @RequierePermiso("INVENTARIO_ATRIBUTO_VER")
    public List<AtributoDeCategoria> listar(UUID categoriaId) {
        categoriaDelNegocio(categoriaId);
        return atributos.findByCategoriaIdOrderByOrdenAscNombreCampoAsc(categoriaId).stream()
                .map(atributo -> comoDto(atributo, null))
                .toList();
    }

    @Transactional
    @RequierePermiso("INVENTARIO_ATRIBUTO_CREAR")
    public AtributoDeCategoria crear(UUID categoriaId, SolicitudDeAtributo solicitud) {
        categoriaDelNegocio(categoriaId);
        validarForma(solicitud);

        if (atributos.existsByCategoriaIdAndNombreCampoIgnoreCase(categoriaId,
                solicitud.nombreCampo())) {
            throw new RecursoDuplicadoException("La categoria ya tiene un atributo \""
                    + solicitud.nombreCampo() + "\"");
        }

        AtributoCategoria atributo = AtributoCategoria.nuevo(ContextoDeNegocio.negocioActual(),
                categoriaId, solicitud.nombreCampo(), solicitud.etiqueta(), solicitud.tipo());
        aplicar(atributo, solicitud);
        atributos.save(atributo);

        return comoDto(atributo, advertenciaPorObligatorio(solicitud, categoriaId));
    }

    @Transactional
    @RequierePermiso("INVENTARIO_ATRIBUTO_EDITAR")
    public AtributoDeCategoria actualizar(UUID atributoId, SolicitudDeAtributo solicitud) {
        AtributoCategoria atributo = atributos.findById(atributoId)
                .orElseThrow(() -> new NoEncontradoException("Ese atributo no existe"));
        categoriaDelNegocio(atributo.getCategoriaId());
        validarForma(solicitud);

        if (!atributo.getNombreCampo().equalsIgnoreCase(solicitud.nombreCampo())
                && atributos.existsByCategoriaIdAndNombreCampoIgnoreCase(atributo.getCategoriaId(),
                        solicitud.nombreCampo())) {
            throw new RecursoDuplicadoException("La categoria ya tiene un atributo \""
                    + solicitud.nombreCampo() + "\"");
        }
        aplicar(atributo, solicitud);
        atributos.save(atributo);
        return comoDto(atributo, advertenciaPorObligatorio(solicitud, atributo.getCategoriaId()));
    }

    @Transactional
    @RequierePermiso("INVENTARIO_ATRIBUTO_EDITAR")
    public void eliminar(UUID atributoId) {
        AtributoCategoria atributo = atributos.findById(atributoId)
                .orElseThrow(() -> new NoEncontradoException("Ese atributo no existe"));
        categoriaDelNegocio(atributo.getCategoriaId());
        atributos.delete(atributo);
    }

    /** Criterio 2 y 5: la forma del atributo tiene que ser coherente. */
    private void validarForma(SolicitudDeAtributo s) {
        if (s.tipo().esDeOpciones() && (s.opciones() == null || s.opciones().isEmpty())) {
            throw new ReglaDeNegocioException(
                    "Un atributo de tipo " + s.tipo() + " necesita al menos una opcion");
        }
        boolean tieneRango = s.valorMin() != null || s.valorMax() != null;
        if (tieneRango && !s.tipo().esNumerico()) {
            throw new ReglaDeNegocioException(
                    "El rango de valores solo aplica a atributos NUMERO o DECIMAL");
        }
        if (s.valorMin() != null && s.valorMax() != null
                && s.valorMin().compareTo(s.valorMax()) > 0) {
            throw new ReglaDeNegocioException("El minimo no puede ser mayor que el maximo");
        }
    }

    private void aplicar(AtributoCategoria atributo, SolicitudDeAtributo s) {
        atributo.configurar(s.obligatorio(), s.unidad(),
                s.tipo().esDeOpciones() ? s.opciones() : null,
                s.valorDefault(), s.validacionRegex(), s.valorMin(), s.valorMax(),
                s.heredable(), s.orden());
    }

    /** Criterio 3: si se marca obligatorio y ya hay productos sin ese valor. */
    private String advertenciaPorObligatorio(SolicitudDeAtributo s, UUID categoriaId) {
        if (!s.obligatorio()) {
            return null;
        }
        long sinValor = categorias.contarProductosSinAtributo(categoriaId, s.nombreCampo());
        if (sinValor == 0) {
            return null;
        }
        return sinValor + " producto(s) de esta categoria no tienen \"" + s.nombreCampo()
                + "\". Conservan lo que tienen; el campo se exige de aqui en adelante";
    }

    private Categoria categoriaDelNegocio(UUID categoriaId) {
        Categoria categoria = categorias.findById(categoriaId)
                .orElseThrow(() -> new NoEncontradoException("Esa categoria no existe"));
        if (!categoria.getNegocioId().equals(ContextoDeNegocio.negocioActual())) {
            throw new NoEncontradoException("Esa categoria no existe");
        }
        return categoria;
    }

    private static AtributoDeCategoria comoDto(AtributoCategoria a, String advertencia) {
        return new AtributoDeCategoria(a.getId(), a.getCategoriaId(), a.getNombreCampo(),
                a.getEtiqueta(), a.getTipo(), a.isObligatorio(), a.getUnidad(), a.getOpciones(),
                a.getValorDefault(), a.getValidacionRegex(), a.getValorMin(), a.getValorMax(),
                a.isHeredable(), a.getOrden(), advertencia);
    }
}
