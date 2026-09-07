package com.regenta.inventario.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.regenta.comun.errores.NoEncontradoException;
import com.regenta.comun.errores.RecursoDuplicadoException;
import com.regenta.comun.errores.ReglaDeNegocioException;
import com.regenta.inventario.BaseDeInventario;
import com.regenta.inventario.domain.TipoAtributo;

/** HU-027. Los atributos que exige cada categoria. */
class GestionDeAtributosTest extends BaseDeInventario {

    private static final Set<String> DE_ADMIN = Set.of("INVENTARIO_CATEGORIA_CREAR",
            "INVENTARIO_ATRIBUTO_VER", "INVENTARIO_ATRIBUTO_CREAR", "INVENTARIO_ATRIBUTO_EDITAR");

    @Autowired
    private GestionDeCategorias categorias;

    @Autowired
    private GestionDeAtributos atributos;

    private final UUID negocioA = UUID.randomUUID();
    private final UUID negocioB = UUID.randomUUID();
    private final UUID admin = UUID.randomUUID();

    private UUID categoriaEn(UUID negocio, String nombre) {
        return enContexto(negocio, admin, DE_ADMIN, () -> categorias
                .crear(new SolicitudDeCategoria(nombre, null, null, null, null, null))).id();
    }

    private AtributoDeCategoria crear(UUID negocio, UUID categoria, SolicitudDeAtributo s) {
        return enContexto(negocio, admin, DE_ADMIN, () -> atributos.crear(categoria, s));
    }

    @Test
    @DisplayName("Criterio 1: se puede elegir cualquiera de los siete tipos")
    void losSieteTipos() {
        UUID categoria = categoriaEn(negocioA, "Varios");

        for (TipoAtributo tipo : TipoAtributo.values()) {
            List<String> opciones = tipo.esDeOpciones() ? List.of("A", "B") : null;
            AtributoDeCategoria creado = crear(negocioA, categoria, new SolicitudDeAtributo(
                    "campo_" + tipo.name().toLowerCase(), tipo.name(), tipo, false, null, opciones,
                    null, null, null, null, null, null));
            assertThat(creado.tipo()).isEqualTo(tipo);
        }
        assertThat(comoElServicio(negocioA, "select count(*) from atributos_categoria"))
                .containsExactly("7");
    }

    @Test
    @DisplayName("Criterio 2: un atributo LISTA sin opciones responde 422")
    void listaSinOpciones() {
        UUID categoria = categoriaEn(negocioA, "Ropa");

        assertThatThrownBy(() -> crear(negocioA, categoria, new SolicitudDeAtributo(
                "talla", "Talla", TipoAtributo.LISTA, false, null, null, null, null, null, null,
                null, null)))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("opcion");
    }

    @Test
    @DisplayName("Criterio 4: nombre_campo repetido en la misma categoria responde 409")
    void nombreCampoRepetido() {
        UUID categoria = categoriaEn(negocioA, "Medicamentos");
        crear(negocioA, categoria, texto("lote"));

        assertThatThrownBy(() -> crear(negocioA, categoria, texto("lote")))
                .isInstanceOf(RecursoDuplicadoException.class);
    }

    @Test
    @DisplayName("Criterio 3: marcar obligatorio con productos sin ese valor advierte y no los toca")
    void obligatorioConProductosSinValorAdvierte() {
        UUID categoria = categoriaEn(negocioA, "Medicamentos");
        UUID unidad = UUID.randomUUID();
        UUID producto = UUID.randomUUID();
        comoSuperusuario("INSERT INTO inventario.unidades_medida (id, negocio_id, codigo, nombre)"
                + " VALUES ('" + unidad + "','" + negocioA + "','UND','Unidad')");
        comoSuperusuario("INSERT INTO inventario.productos (id, negocio_id, sku, nombre,"
                + " categoria_id, unidad_medida_id, atributos) VALUES ('" + producto + "','"
                + negocioA + "','SKU-1','Acetaminofen','" + categoria + "','" + unidad
                + "','{\"registro\":\"INVIMA-1\"}')");

        AtributoDeCategoria creado = crear(negocioA, categoria, new SolicitudDeAtributo(
                "fecha_vencimiento", "Vencimiento", TipoAtributo.FECHA, true, null, null, null,
                null, null, null, null, null));

        assertThat(creado.advertencia()).isNotNull().contains("1 producto");
        assertThat(comoElServicio(negocioA,
                "select atributos->>'registro' from productos where id = '" + producto + "'"))
                .containsExactly("INVIMA-1");
    }

    @Test
    @DisplayName("Sin productos, marcar obligatorio no advierte")
    void obligatorioSinProductosNoAdvierte() {
        UUID categoria = categoriaEn(negocioA, "Tornilleria");

        AtributoDeCategoria creado = crear(negocioA, categoria, new SolicitudDeAtributo(
                "material", "Material", TipoAtributo.TEXTO, true, null, null, null, null, null, null,
                null, null));

        assertThat(creado.advertencia()).isNull();
    }

    @Test
    @DisplayName("Criterio 5 (config): un rango sobre un atributo no numerico responde 422")
    void rangoEnAtributoNoNumerico() {
        UUID categoria = categoriaEn(negocioA, "Herramientas");

        assertThatThrownBy(() -> crear(negocioA, categoria, new SolicitudDeAtributo(
                "nombre_tecnico", "Nombre tecnico", TipoAtributo.TEXTO, false, null, null, null,
                null, BigDecimal.ONE, BigDecimal.TEN, null, null)))
                .isInstanceOf(ReglaDeNegocioException.class);
    }

    @Test
    @DisplayName("Los atributos de una categoria de otro negocio no se ven ni se tocan")
    void atributosDeOtroNegocio() {
        UUID categoriaB = categoriaEn(negocioB, "Cosas de B");

        assertThatThrownBy(() -> enContexto(negocioA, admin, DE_ADMIN,
                () -> atributos.listar(categoriaB)))
                .isInstanceOf(NoEncontradoException.class);
    }

    private static SolicitudDeAtributo texto(String campo) {
        return new SolicitudDeAtributo(campo, campo, TipoAtributo.TEXTO, false, null, null, null,
                null, null, null, null, null);
    }
}
