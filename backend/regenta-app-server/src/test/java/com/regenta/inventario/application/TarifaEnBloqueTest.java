package com.regenta.inventario.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.test.util.ReflectionTestUtils;

import com.regenta.inventario.domain.Producto;
import com.regenta.inventario.domain.TarifaIva;
import com.regenta.inventario.infrastructure.AtributoCategoriaRepositorio;
import com.regenta.inventario.infrastructure.CategoriaRepositorio;
import com.regenta.inventario.infrastructure.ProductoRepositorio;
import com.regenta.inventario.infrastructure.UnidadMedidaRepositorio;
import com.regenta.shared.events.RegistroDeEventos;
import com.regenta.shared.security.GuardiaDePermisos;
import com.regenta.shared.tenancy.ContextoDeNegocio;
import com.regenta.shared.tenancy.DatosDelNegocio;
import com.regenta.shared.web.NoEncontradoException;
import com.regenta.shared.web.SinPermisoException;

/** HU-154 · cambiar la tarifa de IVA en bloque (I2, I6). */
class TarifaEnBloqueTest {

    private final ProductoRepositorio productos = mock(ProductoRepositorio.class);
    private final UUID negocioId = UUID.randomUUID();
    private final UUID pinturas = UUID.randomUUID();
    private Producto a;
    private Producto b;

    private GestionDeProductos gestion(String... permisos) {
        ContextoDeNegocio.establecer(new DatosDelNegocio(negocioId, UUID.randomUUID(), "BASICO",
                "VENTA_DIRECTA", Set.of(), Set.of(), Set.of(permisos), Set.of()));
        AspectJProxyFactory fabrica = new AspectJProxyFactory(new GestionDeProductos(productos,
                mock(CategoriaRepositorio.class), mock(AtributoCategoriaRepositorio.class),
                mock(UnidadMedidaRepositorio.class), mock(ValidadorDeAtributos.class),
                mock(RegistroDeEventos.class)));
        fabrica.addAspect(new GuardiaDePermisos());
        return fabrica.getProxy();
    }

    private Producto migrado(String sku) {
        Producto p = Producto.nuevo(negocioId, sku, sku, pinturas, UUID.randomUUID(),
                TarifaIva.IVA_19, UUID.randomUUID());
        p.datosBasicos(null, null, null, new BigDecimal("35500"), null);
        ReflectionTestUtils.setField(p, "tarifaPorRevisar", true); // como lo dejó HU-147 (I6)
        return p;
    }

    @BeforeEach
    void datos() {
        a = migrado("A");
        b = migrado("B");
        when(productos.findAllById(List.of(a.getId(), b.getId()))).thenReturn(List.of(a, b));
        when(productos.findByNegocioIdAndCategoriaId(negocioId, pinturas)).thenReturn(List.of(a, b));
    }

    @AfterEach
    void limpiar() {
        ContextoDeNegocio.limpiar();
    }

    @Test
    @DisplayName("HU-154 C1 · Los productos seleccionados quedan en IVA_5, sin «por revisar» y con el mismo precio")
    void seleccionadosCambianDeTarifa() {
        int cambiados = gestion("INVENTARIO_PRODUCTO_EDITAR").cambiarTarifaEnBloque(
                new SolicitudDeTarifaEnBloque(List.of(a.getId(), b.getId()), null, TarifaIva.IVA_5));

        assertThat(cambiados).isEqualTo(2);
        for (Producto p : List.of(a, b)) {
            assertThat(p.getTarifaIva()).isEqualTo(TarifaIva.IVA_5);
            assertThat(p.isTarifaPorRevisar()).isFalse();
            assertThat(p.getPrecioVenta()).isEqualByComparingTo("35500");
        }
    }

    @Test
    @DisplayName("HU-154 C1 · Una categoría completa cambia de una vez")
    void categoriaCompleta() {
        int cambiados = gestion("INVENTARIO_PRODUCTO_EDITAR").cambiarTarifaEnBloque(
                new SolicitudDeTarifaEnBloque(List.of(), pinturas, TarifaIva.EXENTO));

        assertThat(cambiados).isEqualTo(2);
        assertThat(a.getTarifaIva()).isEqualTo(TarifaIva.EXENTO);
    }

    @Test
    @DisplayName("HU-154 C2 · Confirmar en bloque los que ya están bien en 19 % solo quita la marca")
    void confirmarSoloQuitaLaMarca() {
        gestion("INVENTARIO_PRODUCTO_EDITAR").cambiarTarifaEnBloque(
                new SolicitudDeTarifaEnBloque(List.of(a.getId(), b.getId()), null, TarifaIva.IVA_19));

        assertThat(a.getTarifaIva()).isEqualTo(TarifaIva.IVA_19);
        assertThat(a.isTarifaPorRevisar()).isFalse();
    }

    @Test
    @DisplayName("HU-154 C3 · Si uno de los productos no es del negocio, no se cambia ninguno")
    void todoONada() {
        UUID ajeno = UUID.randomUUID();
        when(productos.findAllById(List.of(a.getId(), ajeno))).thenReturn(List.of(a));

        assertThatThrownBy(() -> gestion("INVENTARIO_PRODUCTO_EDITAR").cambiarTarifaEnBloque(
                new SolicitudDeTarifaEnBloque(List.of(a.getId(), ajeno), null, TarifaIva.IVA_5)))
                .isInstanceOf(NoEncontradoException.class);
        assertThat(a.getTarifaIva()).isEqualTo(TarifaIva.IVA_19);
        assertThat(a.isTarifaPorRevisar()).isTrue();
    }

    @Test
    @DisplayName("HU-154 · Sin INVENTARIO_PRODUCTO_EDITAR no se cambia (403)")
    void sinPermiso() {
        assertThatThrownBy(() -> gestion("INVENTARIO_PRODUCTO_VER").cambiarTarifaEnBloque(
                new SolicitudDeTarifaEnBloque(List.of(a.getId()), null, TarifaIva.IVA_5)))
                .isInstanceOf(SinPermisoException.class);
    }
}
