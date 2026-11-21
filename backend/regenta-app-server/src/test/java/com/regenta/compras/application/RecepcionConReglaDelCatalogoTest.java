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
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.regenta.compras.domain.OrdenCompraLinea;
import com.regenta.compras.domain.OrdenDeCompra;
import com.regenta.compras.infrastructure.AsignadorDeConsecutivos;
import com.regenta.compras.infrastructure.CuentaPorPagarRepositorio;
import com.regenta.compras.infrastructure.OrdenCompraLineaRepositorio;
import com.regenta.compras.infrastructure.OrdenDeCompraRepositorio;
import com.regenta.compras.infrastructure.ProveedorRepositorio;
import com.regenta.compras.infrastructure.RecepcionLineaRepositorio;
import com.regenta.compras.infrastructure.RecepcionRepositorio;
import com.regenta.inventario.api.InventarioApi;
import com.regenta.inventario.api.ReglaDeLote;
import com.regenta.shared.events.RegistroDeEventos;
import com.regenta.shared.tenancy.ContextoDeNegocio;
import com.regenta.shared.tenancy.DatosDelNegocio;
import com.regenta.shared.web.NoEncontradoException;
import com.regenta.shared.web.ReglaDeNegocioException;

/** HU-165 · Si un producto exige lote lo decide el catálogo, no el cliente (brecha B2). */
class RecepcionConReglaDelCatalogoTest {

    private final RecepcionRepositorio recepciones = mock(RecepcionRepositorio.class);
    private final RecepcionLineaRepositorio recepcionLineas = mock(RecepcionLineaRepositorio.class);
    private final OrdenDeCompraRepositorio ordenes = mock(OrdenDeCompraRepositorio.class);
    private final OrdenCompraLineaRepositorio ordenLineas = mock(OrdenCompraLineaRepositorio.class);
    private final AsignadorDeConsecutivos consecutivos = mock(AsignadorDeConsecutivos.class);
    private final InventarioApi inventario = mock(InventarioApi.class);
    private final GestionDeRecepciones gestion = new GestionDeRecepciones(recepciones,
            recepcionLineas, ordenes, ordenLineas, mock(CuentaPorPagarRepositorio.class),
            mock(ProveedorRepositorio.class),
            mock(com.regenta.compras.infrastructure.ProveedorProductoRepositorio.class),
            consecutivos, mock(RegistroDeEventos.class), inventario, null,
            mock(GestionDeCuentasPorPagar.class), mock(com.regenta.caja.api.CajaApi.class));

    private final UUID negocioId = UUID.randomUUID();
    private final UUID productoId = UUID.randomUUID();
    private OrdenDeCompra orden;
    private OrdenCompraLinea lineaDeOrden;

    @BeforeEach
    void datos() {
        ContextoDeNegocio.establecer(new DatosDelNegocio(negocioId, UUID.randomUUID(), "PROFESIONAL",
                "VENTA_DIRECTA", Set.of(), Set.of("COMPRAS"), Set.of("COMPRAS_COMPRA_CREAR"), Set.of()));
        orden = OrdenDeCompra.crear(negocioId, "OC-1", UUID.randomUUID(), UUID.randomUUID(), null,
                UUID.randomUUID(), null, BigDecimal.ZERO, null);
        orden.aprobar(UUID.randomUUID(), true);
        lineaDeOrden = OrdenCompraLinea.nueva(orden.getId(), negocioId, 1, productoId,
                "Acetaminofén 500 mg", BigDecimal.TEN, new BigDecimal("10500"), BigDecimal.ZERO,
                new BigDecimal("19"));
        when(ordenes.findByIdAndNegocioId(orden.getId(), negocioId)).thenReturn(Optional.of(orden));
        when(ordenLineas.findByOrdenIdOrderByLinea(orden.getId())).thenReturn(List.of(lineaDeOrden));
        when(consecutivos.siguiente(eq(negocioId), any())).thenReturn(1L);
    }

    @AfterEach
    void limpiar() {
        ContextoDeNegocio.limpiar();
    }

    private SolicitudDeRecepcion recibir(boolean clienteDiceExigeLote, String lote, LocalDate vence) {
        return new SolicitudDeRecepcion(orden.getId(), null, "FV-100", null, List.of(
                new SolicitudDeRecepcion.LineaDeRecepcion(lineaDeOrden.getId(), BigDecimal.TEN, null,
                        clienteDiceExigeLote, lote, vence, null)));
    }

    private void catalogo(boolean manejaLotes, boolean perecedero) {
        when(inventario.reglasDeLote(eq(negocioId), anyCollection())).thenReturn(Map.of(productoId,
                new ReglaDeLote(productoId, manejaLotes, perecedero)));
    }

    @Test
    @DisplayName("HU-165 C6 · El cliente dice exigeLote=false, el catálogo dice que maneja lotes: sin lote se rechaza")
    void elCatalogoManda() {
        catalogo(true, false);

        assertThatThrownBy(() -> gestion.crear(recibir(false, null, null)))
                .isInstanceOf(ReglaDeNegocioException.class);
        verify(recepciones, never()).save(any());
    }

    @Test
    @DisplayName("HU-165 C7 · Un perecedero que llega vencido se rechaza")
    void perecederoVencido() {
        catalogo(true, true);

        assertThatThrownBy(() -> gestion.crear(recibir(true, "L-1", LocalDate.now().minusDays(1))))
                .isInstanceOf(ReglaDeNegocioException.class);
    }

    @Test
    @DisplayName("HU-165 · El cliente dice exigeLote=true para un producto sin lotes: se recibe sin lote")
    void sinLotesAunqueElClienteDigaQueSi() {
        catalogo(false, false);

        RecepcionDelNegocio r = gestion.crear(recibir(true, null, null));

        assertThat(r).isNotNull();
        verify(recepciones).save(any());
    }

    @Test
    @DisplayName("HU-165 · Un producto que no está en el catálogo del negocio no se recibe")
    void productoFueraDelCatalogo() {
        when(inventario.reglasDeLote(eq(negocioId), anyCollection())).thenReturn(Map.of());

        assertThatThrownBy(() -> gestion.crear(recibir(false, null, null)))
                .isInstanceOf(NoEncontradoException.class);
    }
}
