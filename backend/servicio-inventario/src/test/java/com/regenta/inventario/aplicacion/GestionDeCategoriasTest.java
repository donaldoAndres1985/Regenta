package com.regenta.inventario.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.regenta.comun.errores.ConflictoDeEstadoException;
import com.regenta.comun.errores.RecursoDuplicadoException;
import com.regenta.comun.errores.SinPermisoException;
import com.regenta.inventario.BaseDeInventario;
import com.regenta.inventario.domain.TipoAtributo;

/** HU-026. Categorias del negocio con jerarquia. */
class GestionDeCategoriasTest extends BaseDeInventario {

    private static final Set<String> DE_ADMIN = Set.of("INVENTARIO_CATEGORIA_VER",
            "INVENTARIO_CATEGORIA_CREAR", "INVENTARIO_CATEGORIA_EDITAR",
            "INVENTARIO_CATEGORIA_ELIMINAR", "INVENTARIO_ATRIBUTO_CREAR");

    @Autowired
    private GestionDeCategorias categorias;

    @Autowired
    private GestionDeAtributos atributos;

    private final UUID negocioA = UUID.randomUUID();
    private final UUID negocioB = UUID.randomUUID();
    private final UUID admin = UUID.randomUUID();

    private CategoriaDelNegocio crearEn(UUID negocio, String nombre, UUID padre) {
        return enContexto(negocio, admin, DE_ADMIN,
                () -> categorias.crear(new SolicitudDeCategoria(nombre, padre, null, null, null, null)));
    }

    @Test
    @DisplayName("Criterio 1: la categoria queda en mi negocio y ningun otro la ve")
    void quedaAisladaPorNegocio() {
        crearEn(negocioA, "Herramientas", null);
        crearEn(negocioB, "Medicamentos", null);

        assertThat(comoElServicio(negocioA, "select nombre from categorias order by nombre"))
                .containsExactly("Herramientas");
        assertThat(comoElServicio(negocioB, "select nombre from categorias order by nombre"))
                .containsExactly("Medicamentos");
    }

    @Test
    @DisplayName("Criterio 2: mismo nombre y mismo padre responde 409")
    void nombreRepetidoConMismoPadre() {
        crearEn(negocioA, "Herramientas", null);

        assertThatThrownBy(() -> crearEn(negocioA, "herramientas", null))
                .isInstanceOf(RecursoDuplicadoException.class);
    }

    @Test
    @DisplayName("Mismo nombre con distinto padre si convive")
    void nombreRepetidoConDistintoPadre() {
        UUID herramientas = crearEn(negocioA, "Herramientas", null).id();
        UUID tornilleria = crearEn(negocioA, "Tornilleria", null).id();

        crearEn(negocioA, "Manuales", herramientas);
        CategoriaDelNegocio otra = crearEn(negocioA, "Manuales", tornilleria);

        assertThat(otra.nivel()).isEqualTo(1);
        assertThat(otra.ruta()).isEqualTo("Tornilleria/Manuales");
    }

    @Test
    @DisplayName("Criterio 3: con productos dentro, no se elimina y pide moverlos")
    void categoriaConProductosNoSeElimina() {
        UUID categoria = crearEn(negocioA, "Herramientas", null).id();
        UUID unidad = UUID.randomUUID();
        comoSuperusuario("INSERT INTO inventario.unidades_medida (id, negocio_id, codigo, nombre)"
                + " VALUES ('" + unidad + "','" + negocioA + "','UND','Unidad')");
        comoSuperusuario("INSERT INTO inventario.productos (id, negocio_id, sku, nombre,"
                + " categoria_id, unidad_medida_id) VALUES ('" + UUID.randomUUID() + "','" + negocioA
                + "','SKU-1','Martillo','" + categoria + "','" + unidad + "')");

        assertThatThrownBy(() -> enContexto(negocioA, admin, DE_ADMIN,
                () -> { categorias.eliminar(categoria); return null; }))
                .isInstanceOf(ConflictoDeEstadoException.class)
                .hasMessageContaining("Muevelos");
    }

    @Test
    @DisplayName("Criterio 3: con subcategorias tampoco se elimina")
    void categoriaConSubcategoriasNoSeElimina() {
        UUID herramientas = crearEn(negocioA, "Herramientas", null).id();
        crearEn(negocioA, "Manuales", herramientas);

        assertThatThrownBy(() -> enContexto(negocioA, admin, DE_ADMIN,
                () -> { categorias.eliminar(herramientas); return null; }))
                .isInstanceOf(ConflictoDeEstadoException.class);
    }

    @Test
    @DisplayName("Criterio 4: la subcategoria hereda los atributos heredables del padre")
    void subcategoriaHeredaAtributosHeredables() {
        UUID medicamentos = crearEn(negocioA, "Medicamentos", null).id();
        enContexto(negocioA, admin, DE_ADMIN, () -> {
            atributos.crear(medicamentos, atributo("lote", TipoAtributo.TEXTO, true));
            atributos.crear(medicamentos, atributo("nota_interna", TipoAtributo.TEXTO, false));
            return null;
        });

        UUID controlados = crearEn(negocioA, "Controlados", medicamentos).id();

        assertThat(comoElServicio(negocioA,
                "select nombre_campo from atributos_categoria where categoria_id = '" + controlados
                        + "' order by nombre_campo"))
                .containsExactly("lote");
    }

    @Test
    @DisplayName("Una categoria vacia se elimina sin problema")
    void categoriaVaciaSeElimina() {
        UUID categoria = crearEn(negocioA, "Papeleria", null).id();

        enContexto(negocioA, admin, DE_ADMIN, () -> { categorias.eliminar(categoria); return null; });

        assertThat(comoElServicio(negocioA, "select count(*) from categorias")).containsExactly("0");
    }

    @Test
    @DisplayName("Sin el permiso de crear, no se crea")
    void sinPermisoNoCrea() {
        assertThatThrownBy(() -> enContexto(negocioA, admin, Set.of("INVENTARIO_CATEGORIA_VER"),
                () -> categorias.crear(new SolicitudDeCategoria("X", null, null, null, null, null))))
                .isInstanceOf(SinPermisoException.class);
    }

    @Test
    @DisplayName("La lista sale ordenada por jerarquia")
    void listaOrdenadaPorJerarquia() {
        UUID a = crearEn(negocioA, "A", null).id();
        crearEn(negocioA, "A-hija", a);
        crearEn(negocioA, "B", null);

        List<CategoriaDelNegocio> lista = enContexto(negocioA, admin, DE_ADMIN, categorias::listar);

        assertThat(lista).extracting(CategoriaDelNegocio::nombre).containsExactly("A", "B", "A-hija");
    }

    private static SolicitudDeAtributo atributo(String campo, TipoAtributo tipo, boolean heredable) {
        return new SolicitudDeAtributo(campo, campo, tipo, false, null, null, null, null, null, null,
                heredable, null);
    }
}
