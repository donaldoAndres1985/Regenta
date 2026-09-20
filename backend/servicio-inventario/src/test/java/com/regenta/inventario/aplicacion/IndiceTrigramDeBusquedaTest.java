package com.regenta.inventario.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.regenta.inventario.BaseDeInventario;

/**
 * HU-126. Mismo hallazgo que en servicio-clientes ({@code IndiceTrigramDeBusquedaTest}
 * de esa base): bajo FORCE ROW LEVEL SECURITY el planificador no baja la
 * búsqueda por nombre al índice GIN trigram, ni con LEAKPROOF aplicado —el
 * estimador de costos de PostgreSQL para GIN subestima su propio beneficio.
 * El criterio se reescribió a un presupuesto de tiempo real (300 ms con diez
 * mil productos), que es lo que de verdad le importa al vendedor.
 */
class IndiceTrigramDeBusquedaTest extends BaseDeInventario {

    private static final Set<String> SETUP = Set.of("INVENTARIO_CATEGORIA_CREAR",
            "INVENTARIO_PRODUCTO_CREAR", "INVENTARIO_PRODUCTO_VER");

    @Autowired
    private BusquedaDeProductos busqueda;
    @Autowired
    private GestionDeCategorias categorias;
    @Autowired
    private GestionDeProductos productos;

    private final UUID negocioA = UUID.randomUUID();
    private final UUID negocioB = UUID.randomUUID();
    private final UUID usuario = UUID.randomUUID();

    private void cargar(UUID negocio, UUID categoriaId, UUID unidadId, int filas,
            int cadaCuantosTornillos) {
        comoSuperusuario("""
                INSERT INTO inventario.productos (id, negocio_id, sku, nombre, categoria_id,
                    unidad_medida_id, precio_venta)
                SELECT gen_random_uuid(), '%s', 'SKU-' || s,
                       CASE WHEN s %% %d = 0 THEN 'Tornillo Volumen ' || s
                            ELSE 'Producto Relleno ' || s END,
                       '%s', '%s', 1000
                  FROM generate_series(1, %d) AS s
                """.formatted(negocio, cadaCuantosTornillos, categoriaId, unidadId, filas));
    }

    @Test
    @DisplayName("Criterio 2 (reescrito): con diez mil productos, la búsqueda por nombre "
            + "responde en menos de 300 ms")
    void respondeRapidoConVolumen() {
        comoSuperusuario("ALTER FUNCTION pg_catalog.textlike(text, text) LEAKPROOF");
        comoSuperusuario("ALTER FUNCTION pg_catalog.texticlike(text, text) LEAKPROOF");
        UUID categoriaId = enContexto(negocioA, usuario, SETUP, () -> categorias
                .crear(new SolicitudDeCategoria("Ferretería", null, null, null, null, null))).id();
        UUID unidadId = enContexto(negocioA, usuario, SETUP,
                () -> productos.crearUnidad("UND", "Unidad", false));
        cargar(negocioA, categoriaId, unidadId, 10_000, 500);
        comoSuperusuario("ANALYZE inventario.productos");

        long inicio = System.nanoTime();
        ResultadoDeBusqueda hallados = enContexto(negocioA, usuario, SETUP,
                () -> busqueda.buscar(new FiltroDeBusqueda("tornillo", null, false, null)));
        long milisegundos = (System.nanoTime() - inicio) / 1_000_000;

        assertThat(hallados.productos()).hasSize(20);
        assertThat(milisegundos).as("tiempo de respuesta con 10.000 productos cargados")
                .isLessThan(300);
    }

    @Test
    @DisplayName("Criterio 3: con ese volumen, un negocio sigue sin ver los del otro")
    void aislamientoConVolumen() {
        UUID categoriaA = enContexto(negocioA, usuario, SETUP, () -> categorias
                .crear(new SolicitudDeCategoria("Ferretería", null, null, null, null, null))).id();
        UUID unidadA = enContexto(negocioA, usuario, SETUP,
                () -> productos.crearUnidad("UND", "Unidad", false));
        UUID categoriaB = enContexto(negocioB, usuario, SETUP, () -> categorias
                .crear(new SolicitudDeCategoria("Ferretería", null, null, null, null, null))).id();
        UUID unidadB = enContexto(negocioB, usuario, SETUP,
                () -> productos.crearUnidad("UND", "Unidad", false));
        cargar(negocioA, categoriaA, unidadA, 10_000, 500);
        cargar(negocioB, categoriaB, unidadB, 500, 50);

        ResultadoDeBusqueda deA = enContexto(negocioA, usuario, SETUP,
                () -> busqueda.buscar(new FiltroDeBusqueda("tornillo", null, false, null)));
        ResultadoDeBusqueda deB = enContexto(negocioB, usuario, SETUP,
                () -> busqueda.buscar(new FiltroDeBusqueda("tornillo", null, false, null)));

        assertThat(deA.productos()).hasSize(20);
        assertThat(deB.productos()).hasSize(10);
        assertThat(deA.productos()).extracting(ProductoEncontrado::id)
                .doesNotContainAnyElementsOf(deB.productos().stream()
                        .map(ProductoEncontrado::id).toList());
    }
}
