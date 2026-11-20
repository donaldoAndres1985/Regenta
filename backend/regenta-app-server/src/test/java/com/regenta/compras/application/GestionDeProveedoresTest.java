package com.regenta.compras.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.regenta.compras.domain.Proveedor;
import com.regenta.compras.domain.TipoDocumentoProveedor;
import com.regenta.compras.infrastructure.CuentaPorPagarRepositorio;
import com.regenta.compras.infrastructure.ProveedorProductoRepositorio;
import com.regenta.compras.infrastructure.ProveedorRepositorio;
import com.regenta.inventario.api.InventarioApi;
import com.regenta.inventario.api.ProductoResumido;
import com.regenta.shared.tenancy.ContextoDeNegocio;
import com.regenta.shared.tenancy.DatosDelNegocio;
import com.regenta.shared.web.ConflictoDeEstadoException;
import com.regenta.shared.web.NoEncontradoException;
import com.regenta.shared.web.RecursoDuplicadoException;

/** HU-166 · proveedores y su catálogo (I1 a I6 de Compras). */
class GestionDeProveedoresTest {

    private final ProveedorRepositorio proveedores = mock(ProveedorRepositorio.class);
    private final ProveedorProductoRepositorio catalogo = mock(ProveedorProductoRepositorio.class);
    private final CuentaPorPagarRepositorio cuentas = mock(CuentaPorPagarRepositorio.class);
    private final InventarioApi inventario = mock(InventarioApi.class);
    private final GestionDeProveedores gestion =
            new GestionDeProveedores(proveedores, catalogo, cuentas, inventario);

    private final UUID negocioId = UUID.randomUUID();

    @BeforeEach
    void contexto() {
        ContextoDeNegocio.establecer(new DatosDelNegocio(negocioId, UUID.randomUUID(), "PROFESIONAL",
                "VENTA_DIRECTA", Set.of(), Set.of("COMPRAS"), Set.of(), Set.of()));
        when(cuentas.saldosAbiertosPorProveedor(negocioId)).thenReturn(List.of());
    }

    @AfterEach
    void limpiar() {
        ContextoDeNegocio.limpiar();
    }

    private Proveedor proveedor(String nombre, String nit) {
        Proveedor p = Proveedor.crear(negocioId, TipoDocumentoProveedor.NIT, nit, nombre, null, null,
                null, null, null, null, 30, new BigDecimal("1000000"), null, null);
        when(proveedores.findByIdAndNegocioId(p.getId(), negocioId)).thenReturn(Optional.of(p));
        return p;
    }

    private SolicitudDeProveedor solicitud(String nit) {
        return new SolicitudDeProveedor("NIT", nit, "Distribuidora Andina SAS", null, null, null, null,
                null, "Medellín", 30, new BigDecimal("1000000"), null, null);
    }

    @Test
    @DisplayName("HU-166 C2 · Un NIT repetido en el negocio responde 409 «Ya existe un proveedor con ese documento.»")
    void documentoRepetido() {
        when(proveedores.existsByNegocioIdAndTipoDocumentoAndNumeroDocumento(negocioId,
                TipoDocumentoProveedor.NIT, "900123456")).thenReturn(true);

        assertThatThrownBy(() -> gestion.crear(solicitud(" 900123456 ")))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessageContaining("Ya existe un proveedor con ese documento.");
        verify(proveedores, never()).save(any());
    }

    @Test
    @DisplayName("HU-166 C3 · Desactivar lo saca de los activos; reactivar lo devuelve")
    void desactivarYReactivar() {
        Proveedor p = proveedor("Distribuidora Andina", "900123456");
        when(proveedores.findByNegocioIdOrderByRazonSocialAsc(negocioId)).thenReturn(List.of(p));

        assertThat(gestion.desactivar(p.getId()).activo()).isFalse();
        assertThat(gestion.listar(GestionDeProveedores.Estado.ACTIVOS)).isEmpty();
        assertThat(gestion.listar(GestionDeProveedores.Estado.INACTIVOS)).hasSize(1);
        assertThat(gestion.ver(p.getId()).activo()).isFalse();

        assertThat(gestion.reactivar(p.getId()).activo()).isTrue();
        assertThat(gestion.listar(GestionDeProveedores.Estado.ACTIVOS)).hasSize(1);
        assertThat(p.estaEliminado()).isFalse();
    }

    @Test
    @DisplayName("HU-166 C3 · Un proveedor desactivado no se edita ni se le cambia el catálogo (409)")
    void desactivadoNoSeEdita() {
        Proveedor p = proveedor("Distribuidora Andina", "900123456");
        p.desactivar();

        assertThatThrownBy(() -> gestion.actualizar(p.getId(), solicitud("900123456")))
                .isInstanceOf(ConflictoDeEstadoException.class);
        assertThatThrownBy(() -> gestion.asociarProducto(p.getId(), new SolicitudDeProductoDeProveedor(
                UUID.randomUUID(), null, BigDecimal.ONE, null, null, false)))
                .isInstanceOf(ConflictoDeEstadoException.class);
    }

    @Test
    @DisplayName("HU-166 C4 · Marcar a B como preferido desmarca al preferido anterior del producto")
    void unSoloPreferido() {
        Proveedor b = proveedor("Proveedor B", "800111222");
        UUID producto = UUID.randomUUID();
        when(inventario.resumenDe(eq(negocioId), anyCollection())).thenReturn(Map.of(producto,
                new ProductoResumido(producto, "ACE-500", "Acetaminofén 500 mg", true)));
        when(catalogo.findByProveedorIdAndProductoId(b.getId(), producto)).thenReturn(Optional.empty());

        ProductoDeProveedor hecho = gestion.asociarProducto(b.getId(),
                new SolicitudDeProductoDeProveedor(producto, "B-77", new BigDecimal("9800"), 3, null, true));

        verify(catalogo).quitarPreferidoDeLosDemas(negocioId, producto, b.getId());
        assertThat(hecho.preferido()).isTrue();
        assertThat(hecho.nombre()).isEqualTo("Acetaminofén 500 mg");
        assertThat(hecho.sku()).isEqualTo("ACE-500");
    }

    @Test
    @DisplayName("HU-166 C4 · Asociar sin marcar preferido no toca a los demás")
    void sinPreferidoNoDesmarca() {
        Proveedor b = proveedor("Proveedor B", "800111222");
        UUID producto = UUID.randomUUID();
        when(inventario.resumenDe(eq(negocioId), anyCollection())).thenReturn(Map.of(producto,
                new ProductoResumido(producto, "ACE-500", "Acetaminofén 500 mg", true)));
        when(catalogo.findByProveedorIdAndProductoId(b.getId(), producto)).thenReturn(Optional.empty());

        gestion.asociarProducto(b.getId(),
                new SolicitudDeProductoDeProveedor(producto, null, new BigDecimal("9800"), null, null, false));

        verify(catalogo, never()).quitarPreferidoDeLosDemas(any(), any(), any());
    }

    @Test
    @DisplayName("HU-166 · Un producto que no está en el catálogo del negocio no se asocia (404)")
    void productoInexistente() {
        Proveedor b = proveedor("Proveedor B", "800111222");
        when(inventario.resumenDe(eq(negocioId), anyCollection())).thenReturn(Map.of());

        assertThatThrownBy(() -> gestion.asociarProducto(b.getId(), new SolicitudDeProductoDeProveedor(
                UUID.randomUUID(), null, BigDecimal.ONE, null, null, true)))
                .isInstanceOf(NoEncontradoException.class);
        verify(catalogo, never()).save(any());
    }

    @Test
    @DisplayName("HU-166 C5 · La deuda es la suma de las cuentas abiertas, no saldo_pendiente")
    void deudaCalculada() {
        Proveedor p = proveedor("Distribuidora Andina", "900123456");
        when(proveedores.findByNegocioIdOrderByRazonSocialAsc(negocioId)).thenReturn(List.of(p));
        when(cuentas.saldosAbiertosPorProveedor(negocioId)).thenReturn(List.<Object[]>of(
                new Object[] { p.getId(), new BigDecimal("800000") }));

        ProveedorDelNegocio visto = gestion.listar(GestionDeProveedores.Estado.ACTIVOS).get(0);

        assertThat(visto.deuda()).isEqualByComparingTo("800000");
        assertThat(visto.superaCupo()).isFalse();
    }

    @Test
    @DisplayName("HU-166 C5 / I6 · Si la deuda pasa el cupo, se informa sin bloquear")
    void superaCupoSoloInforma() {
        Proveedor p = proveedor("Distribuidora Andina", "900123456");
        when(cuentas.saldosAbiertosPorProveedor(negocioId)).thenReturn(List.<Object[]>of(
                new Object[] { p.getId(), new BigDecimal("1200000") }));

        assertThat(gestion.ver(p.getId()).superaCupo()).isTrue();
    }

    @Test
    @DisplayName("HU-166 · El catálogo del proveedor muestra SKU y nombre de cada producto, ordenado por nombre")
    void catalogoConNombres() {
        Proveedor p = proveedor("Distribuidora Andina", "900123456");
        UUID a = UUID.randomUUID();
        UUID z = UUID.randomUUID();
        when(catalogo.findByProveedorId(p.getId())).thenReturn(List.of(
                com.regenta.compras.domain.ProveedorProducto.de(negocioId, p.getId(), z, null,
                        BigDecimal.ONE, null, null, false),
                com.regenta.compras.domain.ProveedorProducto.de(negocioId, p.getId(), a, null,
                        BigDecimal.ONE, null, null, false)));
        when(inventario.resumenDe(eq(negocioId), anyCollection())).thenReturn(Map.of(
                a, new ProductoResumido(a, "A-1", "Alcohol antiséptico", true),
                z, new ProductoResumido(z, "Z-1", "Zinc óxido", true)));

        List<ProductoDeProveedor> productos = gestion.productosDe(p.getId());

        assertThat(productos).extracting(ProductoDeProveedor::nombre)
                .containsExactly("Alcohol antiséptico", "Zinc óxido");
    }
}
