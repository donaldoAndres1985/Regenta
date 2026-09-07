package com.regenta.inventario.infra;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.regenta.inventario.domain.Categoria;

public interface CategoriaRepositorio extends JpaRepository<Categoria, UUID> {

    List<Categoria> findByNegocioIdOrderByNivelAscOrdenAscNombreAsc(UUID negocioId);

    boolean existsByNegocioIdAndCategoriaPadreIdAndNombreIgnoreCase(
            UUID negocioId, UUID categoriaPadreId, String nombre);

    boolean existsByNegocioIdAndCategoriaPadreIdIsNullAndNombreIgnoreCase(
            UUID negocioId, String nombre);

    /** Cuenta los productos de una categoria. La RLS ya lo acota al negocio. */
    @Query(value = "SELECT count(*) FROM inventario.productos WHERE categoria_id = :categoriaId",
            nativeQuery = true)
    long contarProductos(@Param("categoriaId") UUID categoriaId);

    /**
     * Productos de la categoria que no traen esa clave en su JSONB de atributos.
     * Se usa `jsonb_exists(...)` y no el operador `?`, que colisiona con el
     * placeholder de JDBC.
     */
    @Query(value = "SELECT count(*) FROM inventario.productos"
            + " WHERE categoria_id = :categoriaId AND NOT jsonb_exists(atributos, :campo)",
            nativeQuery = true)
    long contarProductosSinAtributo(@Param("categoriaId") UUID categoriaId,
            @Param("campo") String campo);
}
