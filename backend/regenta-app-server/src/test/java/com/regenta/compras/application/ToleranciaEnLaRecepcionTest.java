package com.regenta.compras.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.regenta.caja.api.CajaApi;
import com.regenta.compras.domain.ConfiguracionDeCompras;
import com.regenta.compras.domain.OrdenCompraLinea;
import com.regenta.compras.domain.OrdenDeCompra;
import com.regenta.compras.domain.Proveedor;
import com.regenta.compras.domain.Recepcion;
import com.regenta.compras.domain.RecepcionLinea;
import com.regenta.compras.domain.TipoDocumentoProveedor;
import com.regenta.compras.infrastructure.AsignadorDeConsecutivos;
import com.regenta.compras.infrastructure.ConfiguracionDeComprasRepositorio;
import com.regenta.compras.infrastructure.CuentaPorPagarRepositorio;
import com.regenta.compras.infrastructure.OrdenCompraLineaRepositorio;
import com.regenta.compras.infrastructure.OrdenDeCompraRepositorio;
import com.regenta.compras.infrastructure.ProveedorProductoRepositorio;
import com.regenta.compras.infrastructure.ProveedorRepositorio;
import com.regenta.compras.infrastructure.RecepcionLineaRepositorio;
import com.regenta.compras.infrastructure.RecepcionRepositorio;
import com.regenta.inventario.api.InventarioApi;
import com.regenta.inventario.api.ReglaDeLote;
import com.regenta.shared.events.RegistroDeEventos;
import com.regenta.shared.tenancy.ContextoDeNegocio;
import com.regenta.shared.tenancy.DatosDelNegocio;
import com.regenta.shared.web.ReglaDeNegocioException;

/** HU-173 C4 · Al recibir contra una orden, la tolerancia es la que configuró el negocio. */
class ToleranciaEnLaRecepcionTest {

    private final RecepcionRepositorio recepciones = mock(RecepcionRepositorio.class);
    private final RecepcionLineaRepositorio recepcionLineas = mock(RecepcionLineaRepositorio.class);
    private final OrdenDeCompraRepositorio ordenes = mock(OrdenDeCompraRepositorio.class);
    private final OrdenCompraLineaRepositorio ordenLineas = mock(OrdenCompraLineaRepositorio.class);
    private final ProveedorRepositorio proveedores = mock(ProveedorRepositorio.class);
    private final ConfiguracionDeComprasRepositorio configuraciones = mock(ConfiguracionDeComprasRepositorio.class);
    private final InventarioApi inventario = mock(InventarioApi.class);
    private final AsignadorDeConsecutivos consecutivos = mock(AsignadorDeConsecutivos.class);
    private final GestionDeRecepciones gestion = new GestionDeRecepciones(recepciones, recepcionLineas, ordenes,
            ordenLineas, mock(CuentaPorPagarRepositorio.class), proveedores, mock(ProveedorProductoRepositorio.class),
            consecutivos, mock(RegistroDeEventos.class), inventario, null, mock(GestionDeCuentasPorPagar.class),
            mock(CajaApi.class), new GestionDeConfiguracionDeCompras(configuraciones));

    private final UUID negocioId = UUID.randomUUID();
    private OrdenDeCompra orden;
    private OrdenCompraLinea lineaDeOrden;
    private final List<RecepcionLinea> guardadas = new ArrayList<>();

    @BeforeEach
    void datos() {
        ContextoDeNegocio.establecer(new DatosDelNegocio(negocioId, UUID.randomUUID(), "PROFESIONAL", "VENTA_DIRECTA",
                Set.of(), Set.of("COMPRAS"), Set.of(), Set.of()));
        Proveedor p = Proveedor.crear(negocioId, TipoDocumentoProveedor.NIT, "900123456", "Cementos SAS", null, null,
                null, null, null, null, 30, BigDecimal.ZERO, null, null);
        when(proveedores.findByIdAndNegocioId(p.getId(), negocioId)).thenReturn(Optional.of(p));
        orden = OrdenDeCompra.crear(negocioId, "OC-1", p.getId(), UUID.randomUUID(), null, UUID.randomUUID(), null,
                BigDecimal.ZERO, null);
        orden.aprobar(UUID.randomUUID(), true);
        UUID producto = UUID.randomUUID();
        lineaDeOrden = OrdenCompraLinea.nueva(orden.getId(), negocioId, 1, producto, "Cemento", new BigDecimal("100"),
                new BigDecimal("30000"), BigDecimal.ZERO, BigDecimal.ZERO);
        when(ordenes.findByIdAndNegocioId(orden.getId(), negocioId)).thenReturn(Optional.of(orden));
        when(ordenLineas.findByOrdenIdOrderByLinea(orden.getId())).thenReturn(List.of(lineaDeOrden));
        when(consecutivos.siguiente(eq(negocioId), any())).thenReturn(1L);
        when(inventario.reglasDeLote(eq(negocioId), anyCollection()))
                .thenReturn(Map.of(producto, new ReglaDeLote(producto, false, false)));
        when(recepcionLineas.saveAll(any())).thenAnswer(i -> {
            guardadas.clear();
            i.<Iterable<RecepcionLinea>>getArgument(0).forEach(guardadas::add);
            return guardadas;
        });
    }

    @AfterEach
    void limpiar() {
        ContextoDeNegocio.limpiar();
    }

    private void tolerancia(String pct) {
        ConfiguracionDeCompras c = ConfiguracionDeCompras.porOmision(negocioId);
        c.cambiar(true, null, new BigDecimal(pct));
        when(configuraciones.findById(negocioId)).thenReturn(Optional.of(c));
    }

    private UUID recibir(String cantidad) {
        gestion.crear(new SolicitudDeRecepcion(orden.getId(), null, "FV-1", null, List.of(
                new SolicitudDeRecepcion.LineaDeRecepcion(lineaDeOrden.getId(), new BigDecimal(cantidad), null, false,
                        null, null, null))));
        ArgumentCaptor<Recepcion> r = ArgumentCaptor.forClass(Recepcion.class);
        verify(recepciones, atLeastOnce()).save(r.capture());
        Recepcion recepcion = r.getValue();
        when(recepciones.findByIdAndNegocioId(recepcion.getId(), negocioId)).thenReturn(Optional.of(recepcion));
        when(recepcionLineas.findByRecepcionId(recepcion.getId())).thenReturn(List.copyOf(guardadas));
        return recepcion.getId();
    }

    @Test
    @DisplayName("HU-173 C4 · Con tolerancia 0 %, recibir 101 de 100 pedidas se rechaza")
    void ceroPorCiento() {
        tolerancia("0");

        assertThatThrownBy(() -> recibir("101"))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("tolerancia de 0 %");
    }

    @Test
    @DisplayName("HU-173 C4 · Con tolerancia 10 %, se aceptan 110 de 100 pedidas")
    void diezPorCiento() {
        tolerancia("10");
        UUID recepcion = recibir("110");

        gestion.confirmar(recepcion, null);

        assertThat(lineaDeOrden.getCantidadRecibida()).isEqualByComparingTo("110");
    }

    @Test
    @DisplayName("HU-173 C1 · Sin configuración guardada, la tolerancia es 5 %: 105 sí, 106 no")
    void porOmisionCincoPorCiento() {
        when(configuraciones.findById(negocioId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> recibir("106"))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("tolerancia de 5 %");
    }
}
