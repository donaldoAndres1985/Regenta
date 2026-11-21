package com.regenta.inventario.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

import com.regenta.inventario.api.ProductoNuevo;
import com.regenta.inventario.domain.Categoria;
import com.regenta.inventario.domain.Producto;
import com.regenta.inventario.domain.TarifaIva;
import com.regenta.inventario.domain.UnidadMedida;
import com.regenta.inventario.infrastructure.AtributoCategoriaRepositorio;
import com.regenta.inventario.infrastructure.BodegaRepositorio;
import com.regenta.inventario.infrastructure.CategoriaRepositorio;
import com.regenta.inventario.infrastructure.ProductoRepositorio;
import com.regenta.inventario.infrastructure.UnidadMedidaRepositorio;
import com.regenta.shared.events.RegistroDeEventos;
import com.regenta.shared.security.GuardiaDePermisos;
import com.regenta.shared.tenancy.ContextoDeNegocio;
import com.regenta.shared.tenancy.DatosDelNegocio;
import com.regenta.shared.web.RecursoDuplicadoException;
import com.regenta.shared.web.ReglaDeNegocioException;
import com.regenta.shared.web.SinPermisoException;

/**
 * HU-180 · Compras da de alta un producto por {@code InventarioApi}: pasa por la ficha de Inventario
 * (HU-147) con su permiso, sus duplicados y su tarifa obligatoria, así que responde lo mismo (C2, C4).
 */
class ProductoNuevoDesdeComprasTest {

    private final ProductoRepositorio productos = mock(ProductoRepositorio.class);
    private final CategoriaRepositorio categorias = mock(CategoriaRepositorio.class);
    private final UnidadMedidaRepositorio unidades = mock(UnidadMedidaRepositorio.class);
    private final ValidadorDeAtributos validador = mock(ValidadorDeAtributos.class);

    private final UUID negocioId = UUID.randomUUID();
    private UUID categoriaId;
    private UUID unidadId;

    private InventarioServicio servicio(String... permisos) {
        ContextoDeNegocio.establecer(new DatosDelNegocio(negocioId, UUID.randomUUID(), "PROFESIONAL", "VENTA_DIRECTA",
                Set.of(), Set.of("INVENTARIO", "COMPRAS"), Set.of(permisos), Set.of()));
        GestionDeProductos real = new GestionDeProductos(productos, categorias, mock(AtributoCategoriaRepositorio.class),
                unidades, validador, mock(RegistroDeEventos.class));
        AspectJProxyFactory fabrica = new AspectJProxyFactory(real);
        fabrica.setProxyTargetClass(true);
        fabrica.addAspect(new GuardiaDePermisos());
        GestionDeProductos conGuardia = fabrica.getProxy();
        return new InventarioServicio(mock(GestionDeLotes.class), conGuardia, mock(GestionDeReservasStock.class),
                mock(GestionDeFracciones.class), mock(BodegaRepositorio.class), mock(SalidaPorAnulacionDeCompra.class),
                mock(PreciosParaCompras.class));
    }

    @BeforeEach
    void datos() {
        Categoria categoria = mock(Categoria.class);
        when(categoria.getNegocioId()).thenReturn(negocioId);
        UnidadMedida unidad = UnidadMedida.nueva(negocioId, "UND", "Unidad", false);
        categoriaId = UUID.randomUUID();
        unidadId = unidad.getId();
        when(categorias.findById(categoriaId)).thenReturn(Optional.of(categoria));
        when(unidades.findById(unidadId)).thenReturn(Optional.of(unidad));
        when(validador.validar(any(), any())).thenReturn(Map.of());
        when(productos.save(any())).thenAnswer(i -> i.getArgument(0));
        when(productos.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
    }

    @AfterEach
    void limpiar() {
        ContextoDeNegocio.limpiar();
    }

    private ProductoNuevo nuevo(String tarifa) {
        return new ProductoNuevo("GUA-01", "7701234567890", "Guantes de nitrilo", categoriaId, unidadId, tarifa,
                new BigDecimal("11900"), false, false);
    }

    @Test
    @DisplayName("C1 / C3 · Se crea con el código de barras escaneado, la tarifa y el precio con IVA")
    void crea() {
        UUID id = servicio("INVENTARIO_PRODUCTO_CREAR").crearProducto(negocioId, nuevo("IVA_19"));

        ArgumentCaptor<Producto> guardado = ArgumentCaptor.forClass(Producto.class);
        verify(productos, org.mockito.Mockito.atLeastOnce()).saveAndFlush(guardado.capture());
        Producto p = guardado.getValue();
        assertThat(p.getId()).isEqualTo(id);
        assertThat(p.getSku()).isEqualTo("GUA-01");
        assertThat(p.getCodigoBarras()).isEqualTo("7701234567890");
        assertThat(p.getTarifaIva()).isEqualTo(TarifaIva.IVA_19);
        assertThat(p.getPrecioVenta()).isEqualByComparingTo("11900");
    }

    @Test
    @DisplayName("C2 · Sin tarifa de IVA, el mismo mensaje de Inventario: «Elige la tarifa de IVA del producto.»")
    void sinTarifa() {
        assertThatThrownBy(() -> servicio("INVENTARIO_PRODUCTO_CREAR").crearProducto(negocioId, nuevo(null)))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("Elige la tarifa de IVA del producto.");
    }

    @Test
    @DisplayName("Una tarifa que no existe es 422")
    void tarifaInvalida() {
        assertThatThrownBy(() -> servicio("INVENTARIO_PRODUCTO_CREAR").crearProducto(negocioId, nuevo("IVA_99")))
                .isInstanceOf(ReglaDeNegocioException.class);
    }

    @Test
    @DisplayName("C2 · Un SKU que ya existe se rechaza igual que en la ficha (409)")
    void skuRepetido() {
        when(productos.existsByNegocioIdAndSku(negocioId, "GUA-01")).thenReturn(true);

        assertThatThrownBy(() -> servicio("INVENTARIO_PRODUCTO_CREAR").crearProducto(negocioId, nuevo("IVA_19")))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessageContaining("GUA-01");
    }

    @Test
    @DisplayName("C4 · Sin INVENTARIO_PRODUCTO_CREAR responde 403 y no se crea nada")
    void sinPermiso() {
        assertThatThrownBy(() -> servicio("COMPRAS_COMPRA_CREAR").crearProducto(negocioId, nuevo("IVA_19")))
                .isInstanceOf(SinPermisoException.class);
        verify(productos, never()).saveAndFlush(any());
        verify(productos, never()).save(any());
    }
}
