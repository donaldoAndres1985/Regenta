package com.regenta.compras.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.regenta.compras.domain.Proveedor;
import com.regenta.compras.domain.ProveedorProducto;
import com.regenta.compras.domain.TipoDocumentoProveedor;
import com.regenta.compras.infrastructure.CuentaPorPagarRepositorio;
import com.regenta.compras.infrastructure.ProveedorProductoRepositorio;
import com.regenta.compras.infrastructure.ProveedorRepositorio;
import com.regenta.inventario.api.InventarioApi;
import com.regenta.inventario.api.ProductoNuevo;
import com.regenta.inventario.api.ProductoParaComprar;
import com.regenta.inventario.application.SolicitudDeProducto;
import com.regenta.shared.tenancy.ContextoDeNegocio;
import com.regenta.shared.tenancy.DatosDelNegocio;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

/** HU-180 · Un producto que llega por primera vez se crea desde la recepción y queda con el proveedor (I21). */
class ProductoNuevoEnLaRecepcionTest {

    private final ProveedorRepositorio proveedores = mock(ProveedorRepositorio.class);
    private final ProveedorProductoRepositorio catalogo = mock(ProveedorProductoRepositorio.class);
    private final InventarioApi inventario = mock(InventarioApi.class);
    private final GestionDeProveedores gestion = new GestionDeProveedores(proveedores, catalogo,
            mock(CuentaPorPagarRepositorio.class), inventario);

    private final UUID negocioId = UUID.randomUUID();
    private final UUID productoId = UUID.randomUUID();
    private final UUID categoria = UUID.randomUUID();
    private final UUID unidad = UUID.randomUUID();
    private Proveedor andina;

    @BeforeEach
    void datos() {
        ContextoDeNegocio.establecer(new DatosDelNegocio(negocioId, UUID.randomUUID(), "PROFESIONAL", "VENTA_DIRECTA",
                Set.of(), Set.of("COMPRAS"), Set.of("COMPRAS_COMPRA_CREAR", "INVENTARIO_PRODUCTO_CREAR"), Set.of()));
        andina = Proveedor.crear(negocioId, TipoDocumentoProveedor.NIT, "900123456", "Distribuidora Andina SAS", null,
                null, null, null, null, null, 30, BigDecimal.ZERO, null, null);
        when(proveedores.findByIdAndNegocioId(andina.getId(), negocioId)).thenReturn(Optional.of(andina));
        when(inventario.crearProducto(eq(negocioId), any())).thenReturn(productoId);
        when(inventario.paraComprar(eq(negocioId), anyCollection())).thenReturn(Map.of(productoId,
                new ProductoParaComprar(productoId, "GUA-01", "Guantes de nitrilo", true, "IVA_19",
                        new BigDecimal("19"), false, false)));
        when(catalogo.save(any())).thenAnswer(i -> {
            ProveedorProducto pp = i.getArgument(0);
            when(catalogo.findByProveedorIdAndProductoId(andina.getId(), productoId)).thenReturn(Optional.of(pp));
            return pp;
        });
    }

    @AfterEach
    void limpiar() {
        ContextoDeNegocio.limpiar();
    }

    private SolicitudDeProductoNuevo solicitud(String sku, String tarifa) {
        return new SolicitudDeProductoNuevo(sku, "7701234567890", "Guantes de nitrilo", categoria, unidad, tarifa,
                new BigDecimal("11900"), false, false, "AND-55", new BigDecimal("7000"));
    }

    @Test
    @DisplayName("C3 · Queda creado en Inventario y en el catálogo del proveedor con su código y su costo, listo para la línea")
    void quedaEnElCatalogo() {
        ProductoParaComprarDelProveedor listo = gestion.crearProductoNuevo(andina.getId(), solicitud("GUA-01", "IVA_19"));

        ArgumentCaptor<ProductoNuevo> enviado = ArgumentCaptor.forClass(ProductoNuevo.class);
        verify(inventario).crearProducto(eq(negocioId), enviado.capture());
        assertThat(enviado.getValue().codigoBarras()).isEqualTo("7701234567890");
        assertThat(enviado.getValue().tarifaIva()).isEqualTo("IVA_19");
        ArgumentCaptor<ProveedorProducto> pp = ArgumentCaptor.forClass(ProveedorProducto.class);
        verify(catalogo).save(pp.capture());
        assertThat(pp.getValue().getProductoId()).isEqualTo(productoId);
        assertThat(pp.getValue().getCodigoProveedor()).isEqualTo("AND-55");
        assertThat(pp.getValue().getCostoUltimo()).isEqualByComparingTo("7000");
        assertThat(listo.productoId()).isEqualTo(productoId);
        assertThat(listo.costoSugerido()).isEqualByComparingTo("7000");
        assertThat(listo.codigoProveedor()).isEqualTo("AND-55");
    }

    @Test
    @DisplayName("C2 · Sin SKU, el mensaje de validación es el mismo que el de la ficha de Inventario")
    void mismosMensajes() {
        Validator validador = Validation.buildDefaultValidatorFactory().getValidator();

        Set<String> deCompras = validador.validate(solicitud(" ", "IVA_19")).stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage()).collect(Collectors.toSet());
        Set<String> deInventario = validador.validate(new SolicitudDeProducto(" ", null, "Guantes de nitrilo", null,
                categoria, unidad, null, null, null, null, null, null, null, false, false, false, false, null))
                .stream().map(v -> v.getPropertyPath() + ": " + v.getMessage()).collect(Collectors.toSet());

        assertThat(deCompras).isNotEmpty().isEqualTo(deInventario);
        assertThat(validador.validate(solicitud("GUA-01", null))).extracting(ConstraintViolation::getMessage)
                .isEmpty();
    }
}
