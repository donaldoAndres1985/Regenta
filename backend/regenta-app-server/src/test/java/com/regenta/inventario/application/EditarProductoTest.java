package com.regenta.inventario.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

import com.regenta.inventario.domain.Categoria;
import com.regenta.inventario.domain.Producto;
import com.regenta.inventario.domain.TarifaIva;
import com.regenta.inventario.domain.UnidadMedida;
import com.regenta.inventario.infrastructure.AtributoCategoriaRepositorio;
import com.regenta.inventario.infrastructure.CategoriaRepositorio;
import com.regenta.inventario.infrastructure.ProductoRepositorio;
import com.regenta.inventario.infrastructure.UnidadMedidaRepositorio;
import com.regenta.shared.events.RegistroDeEventos;
import com.regenta.shared.security.GuardiaDePermisos;
import com.regenta.shared.tenancy.ContextoDeNegocio;
import com.regenta.shared.tenancy.DatosDelNegocio;
import com.regenta.shared.web.ConflictoDeEstadoException;
import com.regenta.shared.web.RecursoDuplicadoException;
import com.regenta.shared.web.SinPermisoException;

/** HU-150 · editar un producto desde la ficha (I2, I6, I7). */
class EditarProductoTest {

    private final ProductoRepositorio productos = mock(ProductoRepositorio.class);
    private final CategoriaRepositorio categorias = mock(CategoriaRepositorio.class);
    private final UnidadMedidaRepositorio unidades = mock(UnidadMedidaRepositorio.class);

    private final UUID negocioId = UUID.randomUUID();
    private Producto barniz;
    private UUID categoriaId;
    private UUID unidadId;

    private GestionDeProductos gestion(String... permisos) {
        ContextoDeNegocio.establecer(new DatosDelNegocio(negocioId, UUID.randomUUID(), "BASICO",
                "VENTA_DIRECTA", Set.of(), Set.of(), Set.of(permisos), Set.of()));
        GestionDeProductos real = new GestionDeProductos(productos, categorias,
                mock(AtributoCategoriaRepositorio.class), unidades, mock(ValidadorDeAtributos.class),
                mock(RegistroDeEventos.class));
        AspectJProxyFactory fabrica = new AspectJProxyFactory(real);
        fabrica.addAspect(new GuardiaDePermisos());
        return fabrica.getProxy();
    }

    @BeforeEach
    void datos() {
        Categoria categoria = mock(Categoria.class);
        when(categoria.getNegocioId()).thenReturn(negocioId);
        UnidadMedida unidad = UnidadMedida.nueva(negocioId, "GLN", "Galón", true);
        categoriaId = UUID.randomUUID();
        unidadId = unidad.getId();
        when(categorias.findById(categoriaId)).thenReturn(Optional.of(categoria));
        when(unidades.findById(unidadId)).thenReturn(Optional.of(unidad));

        barniz = Producto.nuevo(negocioId, "7707031403512", "Barniz galón", categoriaId, unidadId,
                TarifaIva.IVA_19, UUID.randomUUID());
        barniz.datosBasicos(null, null, null, new BigDecimal("35500"), null);
        barniz.registrarCostoDeEntrada(new BigDecimal("26500"));
        when(productos.findById(barniz.getId())).thenReturn(Optional.of(barniz));
        when(productos.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
    }

    @AfterEach
    void limpiar() {
        ContextoDeNegocio.limpiar();
    }

    private SolicitudDeEdicion edicion(String sku, String codigo, TarifaIva tarifa, String precio,
            long version) {
        return new SolicitudDeEdicion(sku, codigo, "Barniz galón", categoriaId, unidadId, tarifa,
                new BigDecimal(precio), new BigDecimal("26500"), version);
    }

    @Test
    @DisplayName("HU-150 C1 · Al cambiar el precio a $ 38.000 se guarda y la ficha muestra base, IVA, margen y beneficio")
    void cambiarElPrecioMuestraMargenYBeneficio() {
        ProductoDelNegocio ficha = gestion("INVENTARIO_PRODUCTO_EDITAR").editar(barniz.getId(),
                edicion("7707031403512", null, TarifaIva.IVA_19, "38000", barniz.getVersion()));

        assertThat(ficha.precioVenta()).isEqualByComparingTo("38000");
        assertThat(ficha.base()).isEqualByComparingTo("31932.77");
        assertThat(ficha.iva()).isEqualByComparingTo("6067.23");
        assertThat(ficha.costo()).isEqualByComparingTo("26500");
        assertThat(ficha.margenPct()).isEqualByComparingTo("20.50");
        assertThat(ficha.beneficioPct()).isEqualByComparingTo("17.01");
    }

    @Test
    @DisplayName("HU-150 C2 · Un SKU que ya usa otro producto se rechaza con «Ese SKU ya lo usa otro producto.»")
    void skuRepetidoSeRechaza() {
        when(productos.existsByNegocioIdAndSkuAndIdNot(negocioId, "OTRO-1", barniz.getId()))
                .thenReturn(true);

        assertThatThrownBy(() -> gestion("INVENTARIO_PRODUCTO_EDITAR").editar(barniz.getId(),
                edicion("OTRO-1", null, TarifaIva.IVA_19, "35500", barniz.getVersion())))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessageContaining("Ese SKU ya lo usa otro producto.");
    }

    @Test
    @DisplayName("HU-150 C2 · Un código de barras que ya usa otro producto se rechaza")
    void codigoRepetidoSeRechaza() {
        when(productos.existsByNegocioIdAndCodigoBarrasAndIdNot(negocioId, "770999", barniz.getId()))
                .thenReturn(true);

        assertThatThrownBy(() -> gestion("INVENTARIO_PRODUCTO_EDITAR").editar(barniz.getId(),
                edicion("7707031403512", "770999", TarifaIva.IVA_19, "35500", barniz.getVersion())))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessageContaining("Ese código de barras ya lo usa otro producto.");
    }

    @Test
    @DisplayName("HU-150 C3 · Guardar la ficha con su tarifa quita «tarifa por revisar»")
    void guardarConfirmaLaTarifa() {
        ProductoDelNegocio ficha = gestion("INVENTARIO_PRODUCTO_EDITAR").editar(barniz.getId(),
                edicion("7707031403512", null, TarifaIva.IVA_5, "35500", barniz.getVersion()));

        assertThat(ficha.tarifaIva()).isEqualTo(TarifaIva.IVA_5);
        assertThat(ficha.tarifaPorRevisar()).isFalse();
    }

    @Test
    @DisplayName("HU-150 C4 · Sin INVENTARIO_PRODUCTO_EDITAR no se puede editar (403)")
    void sinPermisoNoSeEdita() {
        assertThatThrownBy(() -> gestion("INVENTARIO_PRODUCTO_VER").editar(barniz.getId(),
                edicion("7707031403512", null, TarifaIva.IVA_19, "38000", barniz.getVersion())))
                .isInstanceOf(SinPermisoException.class);
    }

    @Test
    @DisplayName("HU-150 C5 · Guardar con una versión vieja se rechaza con «Alguien cambió este producto; recarga antes de guardar.»")
    void versionViejaSeRechaza() {
        assertThatThrownBy(() -> gestion("INVENTARIO_PRODUCTO_EDITAR").editar(barniz.getId(),
                edicion("7707031403512", null, TarifaIva.IVA_19, "38000", barniz.getVersion() + 1)))
                .isInstanceOf(ConflictoDeEstadoException.class)
                .hasMessageContaining("Alguien cambió este producto; recarga antes de guardar.");
    }

    @Test
    @DisplayName("HU-150 · La ficha lista las unidades de medida del negocio para elegir")
    void listaLasUnidades() {
        when(unidades.findByNegocioIdOrderByCodigo(negocioId)).thenReturn(List.of(
                UnidadMedida.nueva(negocioId, "GLN", "Galón", true)));

        assertThat(gestion("INVENTARIO_PRODUCTO_VER").unidades()).extracting(UnidadMedida::getCodigo)
                .containsExactly("GLN");
    }
}
