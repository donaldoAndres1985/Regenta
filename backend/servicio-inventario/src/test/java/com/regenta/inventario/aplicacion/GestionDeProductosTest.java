package com.regenta.inventario.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import com.regenta.comun.errores.RecursoDuplicadoException;
import com.regenta.comun.errores.ReglaDeNegocioException;
import com.regenta.inventario.BaseDeInventario;
import com.regenta.inventario.domain.TipoAtributo;

/** HU-028. Crear producto con validacion de atributos dinamicos. */
class GestionDeProductosTest extends BaseDeInventario {

    private static final Set<String> DE_ADMIN = Set.of("INVENTARIO_CATEGORIA_CREAR",
            "INVENTARIO_ATRIBUTO_CREAR", "INVENTARIO_PRODUCTO_CREAR", "INVENTARIO_PRODUCTO_VER");

    @Autowired
    private GestionDeProductos productos;

    @Autowired
    private GestionDeCategorias categorias;

    @Autowired
    private GestionDeAtributos atributos;

    private final UUID negocio = UUID.randomUUID();
    private final UUID usuario = UUID.randomUUID();

    private UUID categoria;
    private UUID unidad;

    @BeforeEach
    void preparar() {
        enContexto(negocio, usuario, DE_ADMIN, () -> {
            categoria = categorias
                    .crear(new SolicitudDeCategoria("Medicamentos", null, null, null, null, null))
                    .id();
            unidad = productos.crearUnidad("UND", "Unidad", false);
            return null;
        });
    }

    private SolicitudDeProducto producto(String sku, Map<String, Object> atrs) {
        return new SolicitudDeProducto(sku, null, "Acetaminofen 500", null, categoria, unidad, null,
                null, new BigDecimal("1200"), null, null, null, false, false, false, false, atrs);
    }

    private void exige(String campo, TipoAtributo tipo) {
        enContexto(negocio, usuario, DE_ADMIN, () -> atributos.crear(categoria,
                new SolicitudDeAtributo(campo, campo, tipo, true, null, null, null, null, null, null,
                        null, null)));
    }

    @Test
    @DisplayName("Criterio 1: sin los atributos obligatorios, 422 nombrando los campos")
    void faltanAtributosObligatorios() {
        exige("lote", TipoAtributo.TEXTO);
        exige("fecha_vencimiento", TipoAtributo.FECHA);

        assertThatThrownBy(() -> enContexto(negocio, usuario, DE_ADMIN,
                () -> productos.crear(producto("SKU-1", Map.of()))))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("lote")
                .hasMessageContaining("fecha_vencimiento");
    }

    @Test
    @DisplayName("Criterio 2: un producto valido guarda sus atributos en el JSONB, no en columnas")
    void atributosEnJsonb() {
        exige("lote", TipoAtributo.TEXTO);
        exige("fecha_vencimiento", TipoAtributo.FECHA);

        UUID id = enContexto(negocio, usuario, DE_ADMIN, () -> productos.crear(producto("SKU-1",
                Map.of("lote", "L-42", "fecha_vencimiento", "2027-01-01")))).id();

        assertThat(comoElServicio(negocio,
                "select atributos->>'lote' from productos where id = '" + id + "'"))
                .containsExactly("L-42");
        assertThat(comoElServicio(negocio,
                "select atributos->>'fecha_vencimiento' from productos where id = '" + id + "'"))
                .containsExactly("2027-01-01");
    }

    @Test
    @DisplayName("Criterio 3: SKU repetido en el negocio responde 409")
    void skuRepetido() {
        enContexto(negocio, usuario, DE_ADMIN,
                () -> productos.crear(producto("SKU-1", Map.of())));

        assertThatThrownBy(() -> enContexto(negocio, usuario, DE_ADMIN,
                () -> productos.crear(producto("SKU-1", Map.of()))))
                .isInstanceOf(RecursoDuplicadoException.class);
    }

    @Test
    @DisplayName("Criterio 4: codigo de barras repetido 409; pero dos sin codigo conviven")
    void codigoDeBarras() {
        SolicitudDeProducto conCodigo = new SolicitudDeProducto("SKU-1", "7701234567890",
                "A", null, categoria, unidad, null, null, BigDecimal.ONE, null, null, null,
                false, false, false, false, Map.of());
        SolicitudDeProducto mismoCodigo = new SolicitudDeProducto("SKU-2", "7701234567890",
                "B", null, categoria, unidad, null, null, BigDecimal.ONE, null, null, null,
                false, false, false, false, Map.of());

        enContexto(negocio, usuario, DE_ADMIN, () -> productos.crear(conCodigo));
        assertThatThrownBy(() -> enContexto(negocio, usuario, DE_ADMIN,
                () -> productos.crear(mismoCodigo)))
                .isInstanceOf(RecursoDuplicadoException.class);

        enContexto(negocio, usuario, DE_ADMIN, () -> productos.crear(producto("SKU-3", Map.of())));
        enContexto(negocio, usuario, DE_ADMIN, () -> productos.crear(producto("SKU-4", Map.of())));
        assertThat(comoElServicio(negocio,
                "select count(*) from productos where codigo_barras is null"))
                .containsExactly("2");
    }

    @Test
    @DisplayName("Criterio 5: perecedero sin maneja_lotes lo rechaza el CHECK de la base")
    void perecederoSinLotes() {
        SolicitudDeProducto malo = new SolicitudDeProducto("SKU-P", null, "Yogur", null,
                categoria, unidad, null, null, BigDecimal.ONE, null, null, null,
                false, false, true, false, Map.of());

        assertThatThrownBy(() -> enContexto(negocio, usuario, DE_ADMIN,
                () -> productos.crear(malo)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Criterio 6: un atributo NUMERO con texto responde 422")
    void numeroConTexto() {
        exige("calibre", TipoAtributo.NUMERO);

        assertThatThrownBy(() -> enContexto(negocio, usuario, DE_ADMIN,
                () -> productos.crear(producto("SKU-1", Map.of("calibre", "grueso")))))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("calibre");
    }

    @Test
    @DisplayName("Un producto de una categoria de otro negocio no se crea")
    void categoriaDeOtroNegocio() {
        UUID otro = UUID.randomUUID();
        assertThatThrownBy(() -> enContexto(otro, usuario, DE_ADMIN,
                () -> productos.crear(producto("SKU-X", Map.of()))))
                .hasMessageContaining("categoria");
    }
}
