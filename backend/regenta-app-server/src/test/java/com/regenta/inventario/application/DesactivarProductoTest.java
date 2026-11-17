package com.regenta.inventario.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.web.bind.annotation.DeleteMapping;

import com.regenta.inventario.domain.Producto;
import com.regenta.inventario.domain.TarifaIva;
import com.regenta.inventario.infrastructure.AtributoCategoriaRepositorio;
import com.regenta.inventario.infrastructure.CategoriaRepositorio;
import com.regenta.inventario.infrastructure.ExistenciaRepositorio;
import com.regenta.inventario.infrastructure.ProductoCodigoRepositorio;
import com.regenta.inventario.infrastructure.ProductoRepositorio;
import com.regenta.inventario.infrastructure.UnidadMedidaRepositorio;
import com.regenta.inventario.web.ProductosControlador;
import com.regenta.shared.events.RegistroDeEventos;
import com.regenta.shared.security.GuardiaDePermisos;
import com.regenta.shared.tenancy.ContextoDeNegocio;
import com.regenta.shared.tenancy.DatosDelNegocio;
import com.regenta.shared.web.NoEncontradoException;
import com.regenta.shared.web.SinPermisoException;

/** HU-151 · desactivar y reactivar un producto; nunca se borra (I8). */
class DesactivarProductoTest {

    private final ProductoRepositorio productos = mock(ProductoRepositorio.class);
    private final UUID negocioId = UUID.randomUUID();
    private Producto barniz;

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

    @BeforeEach
    void datos() {
        barniz = Producto.nuevo(negocioId, "7707031403512", "Barniz galón", UUID.randomUUID(),
                UUID.randomUUID(), TarifaIva.IVA_19, UUID.randomUUID());
        barniz.datosBasicos(null, null, null, null, "7707031403512");
        when(productos.findById(barniz.getId())).thenReturn(Optional.of(barniz));
        when(productos.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
    }

    @AfterEach
    void limpiar() {
        ContextoDeNegocio.limpiar();
    }

    @Test
    @DisplayName("HU-151 C1 · Desactivar deja el producto inactivo")
    void desactivar() {
        ProductoDelNegocio p = gestion("INVENTARIO_PRODUCTO_EDITAR").desactivar(barniz.getId());

        assertThat(p.activo()).isFalse();
    }

    @Test
    @DisplayName("HU-151 C2 · Un producto inactivo no se encuentra por código de barras en el POS")
    void inactivoNoSeEncuentraPorCodigo() {
        barniz.desactivar();
        when(productos.findByNegocioIdAndCodigoBarras(negocioId, "7707031403512"))
                .thenReturn(Optional.of(barniz));
        gestion("INVENTARIO_PRODUCTO_VER");
        BusquedaDeProductos busqueda = new BusquedaDeProductos(productos,
                mock(ProductoCodigoRepositorio.class), mock(CategoriaRepositorio.class),
                mock(ExistenciaRepositorio.class));

        assertThatThrownBy(() -> busqueda.resolverCodigo("7707031403512"))
                .isInstanceOf(NoEncontradoException.class);
    }

    @Test
    @DisplayName("HU-151 C3 · Un producto inactivo se sigue viendo en su ficha")
    void inactivoSeVeEnLaFicha() {
        barniz.desactivar();

        assertThat(gestion("INVENTARIO_PRODUCTO_VER").ver(barniz.getId()).activo()).isFalse();
    }

    @Test
    @DisplayName("HU-151 C4 · Reactivar lo vuelve a dejar activo")
    void reactivar() {
        barniz.desactivar();

        assertThat(gestion("INVENTARIO_PRODUCTO_EDITAR").reactivar(barniz.getId()).activo()).isTrue();
    }

    @Test
    @DisplayName("HU-151 · Sin INVENTARIO_PRODUCTO_EDITAR no se desactiva (403)")
    void sinPermisoNoSeDesactiva() {
        assertThatThrownBy(() -> gestion("INVENTARIO_PRODUCTO_VER").desactivar(barniz.getId()))
                .isInstanceOf(SinPermisoException.class);
    }

    @Test
    @DisplayName("HU-151 C5 · La API de productos no tiene forma de borrar un producto")
    void noHayBorrado() {
        List<Method> borrados = Arrays.stream(ProductosControlador.class.getDeclaredMethods())
                .filter(m -> m.isAnnotationPresent(DeleteMapping.class)).toList();

        assertThat(borrados).isEmpty();
        assertThat(Arrays.stream(GestionDeProductos.class.getDeclaredMethods())
                .map(Method::getName)).noneMatch(n -> n.startsWith("borrar") || n.startsWith("eliminar"));
    }
}
