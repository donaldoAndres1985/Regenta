package com.regenta.inventario.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.regenta.comun.errores.RecursoDuplicadoException;
import com.regenta.comun.errores.ReglaDeNegocioException;
import com.regenta.inventario.BaseDeInventario;

/** HU-036. Listas de precios y precios por volumen. */
class GestionDeListasDePreciosTest extends BaseDeInventario {

    private static final Set<String> SETUP = Set.of("INVENTARIO_CATEGORIA_CREAR",
            "INVENTARIO_PRODUCTO_CREAR", "INVENTARIO_LISTA_PRECIOS_GESTIONAR",
            "INVENTARIO_LISTA_PRECIOS_VER");

    @Autowired
    private GestionDeListasDePrecios listas;

    @Autowired
    private GestionDeCategorias categorias;

    @Autowired
    private GestionDeProductos productos;

    private final UUID negocio = UUID.randomUUID();
    private final UUID usuario = UUID.randomUUID();

    private UUID producto;

    @BeforeEach
    void preparar() {
        enContexto(negocio, usuario, SETUP, () -> {
            UUID categoria = categorias
                    .crear(new SolicitudDeCategoria("Cat", null, null, null, null, null)).id();
            UUID unidad = productos.crearUnidad("UND", "Unidad", false);
            producto = productos.crear(new SolicitudDeProducto("SKU-P", null, "Prod", null,
                    categoria, unidad, null, null, new BigDecimal("10"), null, null, null,
                    false, false, false, false, null)).id();
            return null;
        });
    }

    private UUID crearLista(String nombre, LocalDate desde, LocalDate hasta) {
        return enContexto(negocio, usuario, SETUP, () -> listas.crearLista(
                new SolicitudDeLista(nombre, "COP", false, desde, hasta)).id());
    }

    private void fijarPrecio(UUID lista, String precio, String descuentoMax, String cantidadMinima) {
        enContexto(negocio, usuario, SETUP, () -> {
            listas.fijarPrecio(new SolicitudDePrecio(lista, producto, new BigDecimal(precio),
                    new BigDecimal(descuentoMax), new BigDecimal(cantidadMinima)));
            return null;
        });
    }

    @Test
    @DisplayName("Criterio 1: el precio sale de la lista, no del precio base del producto")
    void elPrecioSaleDeLaLista() {
        UUID lista = crearLista("Mayorista", null, null);
        fijarPrecio(lista, "8.0000", "0", "1");

        PrecioResuelto r = enContexto(negocio, usuario, SETUP,
                () -> listas.resolverPrecio(lista, producto, new BigDecimal("3")));

        assertThat(r.precio()).isEqualByComparingTo("8");
    }

    @Test
    @DisplayName("Criterio 2: un precio por volumen desde 12 aplica cuando se venden 15")
    void precioPorVolumen() {
        UUID lista = crearLista("Mayorista", null, null);
        fijarPrecio(lista, "8.0000", "0", "1");
        fijarPrecio(lista, "6.5000", "0", "12");

        PrecioResuelto quince = enContexto(negocio, usuario, SETUP,
                () -> listas.resolverPrecio(lista, producto, new BigDecimal("15")));
        PrecioResuelto diez = enContexto(negocio, usuario, SETUP,
                () -> listas.resolverPrecio(lista, producto, new BigDecimal("10")));

        assertThat(quince.precio()).isEqualByComparingTo("6.5");
        assertThat(quince.cantidadMinimaAplicada()).isEqualByComparingTo("12");
        assertThat(diez.precio()).isEqualByComparingTo("8");
        assertThat(diez.cantidadMinimaAplicada()).isEqualByComparingTo("1");
    }

    @Test
    @DisplayName("Criterio 3: una lista con vigencia vencida no aparece entre las opciones ni se puede usar")
    void listaVencidaNoAparece() {
        UUID vigente = crearLista("Vigente", null, LocalDate.now().plusDays(30));
        UUID vencida = crearLista("Vencida", null, LocalDate.now().minusDays(1));
        fijarPrecio(vencida, "7.0000", "0", "1");

        assertThat(enContexto(negocio, usuario, SETUP, () -> listas.listasVigentes()))
                .extracting(ListaDelNegocio::id)
                .contains(vigente)
                .doesNotContain(vencida);

        assertThatThrownBy(() -> enContexto(negocio, usuario, SETUP,
                () -> listas.resolverPrecio(vencida, producto, new BigDecimal("1"))))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("vig");
    }

    @Test
    @DisplayName("Criterio 4: un descuento mayor al máximo de la lista se rechaza")
    void descuentoSobreElMaximoSeRechaza() {
        UUID lista = crearLista("Mayorista", null, null);
        fijarPrecio(lista, "8.0000", "10", "1");

        assertThatThrownBy(() -> enContexto(negocio, usuario, SETUP, () -> listas.validarDescuento(
                lista, producto, new BigDecimal("1"), new BigDecimal("15"))))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("descuento");

        assertThatCode(() -> enContexto(negocio, usuario, SETUP, () -> listas.validarDescuento(
                lista, producto, new BigDecimal("1"), new BigDecimal("8"))))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Un nombre de lista repetido en el negocio se rechaza")
    void nombreRepetidoSeRechaza() {
        crearLista("Mayorista", null, null);
        assertThatThrownBy(() -> crearLista("Mayorista", null, null))
                .isInstanceOf(RecursoDuplicadoException.class);
    }

    @Test
    @DisplayName("Las listas de otro negocio no se mezclan")
    void aisladoPorNegocio() {
        crearLista("Mayorista", null, null);
        assertThat(comoElServicio(UUID.randomUUID(), "select count(*) from listas_precios"))
                .containsExactly("0");
    }
}
