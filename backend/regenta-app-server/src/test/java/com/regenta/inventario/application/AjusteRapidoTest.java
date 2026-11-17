package com.regenta.inventario.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.regenta.inventario.domain.AjusteLinea;
import com.regenta.inventario.domain.Bodega;
import com.regenta.inventario.domain.Existencia;
import com.regenta.inventario.domain.Producto;
import com.regenta.inventario.domain.TarifaIva;
import com.regenta.inventario.domain.TipoAjuste;
import com.regenta.inventario.domain.TipoMovimiento;
import com.regenta.inventario.infrastructure.AjusteLineaRepositorio;
import com.regenta.inventario.infrastructure.AjusteRepositorio;
import com.regenta.inventario.infrastructure.BodegaRepositorio;
import com.regenta.inventario.infrastructure.ExistenciaLoteRepositorio;
import com.regenta.inventario.infrastructure.ExistenciaRepositorio;
import com.regenta.inventario.infrastructure.LoteRepositorio;
import com.regenta.inventario.infrastructure.ProductoRepositorio;
import com.regenta.shared.tenancy.ContextoDeNegocio;
import com.regenta.shared.tenancy.DatosDelNegocio;
import com.regenta.shared.web.ReglaDeNegocioException;

/** HU-159 · sumar, restar o fijar por conteo físico la existencia, con motivo (I10, I11). */
class AjusteRapidoTest {

    private final AjusteRepositorio ajustes = mock(AjusteRepositorio.class);
    private final AjusteLineaRepositorio lineas = mock(AjusteLineaRepositorio.class);
    private final LibroMayorDeInventario libro = mock(LibroMayorDeInventario.class);
    private final ExistenciaRepositorio existencias = mock(ExistenciaRepositorio.class);
    private final BodegaRepositorio bodegas = mock(BodegaRepositorio.class);
    private final ProductoRepositorio productos = mock(ProductoRepositorio.class);
    private final GestionDeAjustes gestion = new GestionDeAjustes(ajustes, lineas, libro, existencias,
            mock(ExistenciaLoteRepositorio.class), bodegas, productos, mock(LoteRepositorio.class));

    private final UUID negocioId = UUID.randomUUID();
    private final UUID bodegaId = UUID.randomUUID();
    private final List<AjusteLinea> guardadas = new ArrayList<>();
    private Producto barniz;

    @BeforeEach
    void datos() {
        ContextoDeNegocio.establecer(new DatosDelNegocio(negocioId, UUID.randomUUID(), "BASICO",
                "VENTA_DIRECTA", Set.of(), Set.of(), Set.of("INVENTARIO_AJUSTE_APLICAR"), Set.of()));
        barniz = Producto.nuevo(negocioId, "BAR-1", "Barniz galón", UUID.randomUUID(), UUID.randomUUID(),
                TarifaIva.IVA_19, UUID.randomUUID());
        barniz.configuracionDeStock(true, new BigDecimal("2"), new BigDecimal("20"), false, false, false, false);
        when(productos.findById(barniz.getId())).thenReturn(Optional.of(barniz));
        Bodega principal = mock(Bodega.class);
        when(principal.getId()).thenReturn(bodegaId);
        when(principal.getNegocioId()).thenReturn(negocioId);
        when(principal.getNombre()).thenReturn("Principal");
        when(bodegas.findById(bodegaId)).thenReturn(Optional.of(principal));
        when(ajustes.save(any())).thenAnswer(i -> i.getArgument(0));
        when(lineas.save(any())).thenAnswer(i -> {
            guardadas.add(i.getArgument(0));
            return i.getArgument(0);
        });
        when(lineas.findByAjusteIdOrderById(any())).thenAnswer(i -> List.copyOf(guardadas));
    }

    @AfterEach
    void limpiar() {
        ContextoDeNegocio.limpiar();
    }

    private void hay(String cantidad) {
        Existencia e = Existencia.enCero(negocioId, barniz.getId(), bodegaId);
        BigDecimal c = new BigDecimal(cantidad);
        e.aplicar(c.signum() < 0 ? -1 : 1, c.abs());
        when(existencias.findByProductoIdAndBodegaId(barniz.getId(), bodegaId)).thenReturn(Optional.of(e));
    }

    private SolicitudDeMovimiento movimiento() {
        ArgumentCaptor<SolicitudDeMovimiento> m = ArgumentCaptor.forClass(SolicitudDeMovimiento.class);
        verify(libro).registrar(m.capture());
        return m.getValue();
    }

    @Test
    @DisplayName("HU-159 C1 · Con 8 unidades, una salida de 5 por avería deja 3 y queda en el historial con su motivo")
    void restarPorAveria() {
        hay("8");

        gestion.ajusteRapido(new SolicitudDeAjusteRapido(barniz.getId(), bodegaId,
                SolicitudDeAjusteRapido.Operacion.RESTAR, new BigDecimal("5"), TipoAjuste.AVERIA, "Se cayó del estante"));

        SolicitudDeMovimiento m = movimiento();
        assertThat(m.tipo()).isEqualTo(TipoMovimiento.SALIDA_AJUSTE);
        assertThat(m.cantidad()).isEqualByComparingTo("5");
        assertThat(m.motivo()).contains("AVERIA").contains("Se cayó del estante");
    }

    @Test
    @DisplayName("HU-159 C2 · Con 3 unidades, restar 5 se rechaza con el mensaje exacto y nada cambia")
    void noQuedaEnNegativo() {
        hay("3");

        assertThatThrownBy(() -> gestion.ajusteRapido(new SolicitudDeAjusteRapido(barniz.getId(), bodegaId,
                SolicitudDeAjusteRapido.Operacion.RESTAR, new BigDecimal("5"), TipoAjuste.MERMA, "x")))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("Solo hay 3 unidades de Barniz galón en Principal. No puedes sacar 5.");
        verify(libro, never()).registrar(any());
        verify(ajustes, never()).save(any());
    }

    @Test
    @DisplayName("HU-159 C3 · Con existencia −2, un conteo físico de 4 la deja en 4 con una entrada de 6")
    void conteoFisicoCorrigeElNegativo() {
        hay("-2");

        gestion.ajusteRapido(new SolicitudDeAjusteRapido(barniz.getId(), bodegaId,
                SolicitudDeAjusteRapido.Operacion.CONTEO, new BigDecimal("4"), TipoAjuste.CONTEO_FISICO, "Inventario de fin de mes"));

        SolicitudDeMovimiento m = movimiento();
        assertThat(m.tipo()).isEqualTo(TipoMovimiento.ENTRADA_AJUSTE);
        assertThat(m.cantidad()).isEqualByComparingTo("6");
    }

    @Test
    @DisplayName("HU-159 · Sumar entra como entrada de ajuste")
    void sumar() {
        hay("8");

        gestion.ajusteRapido(new SolicitudDeAjusteRapido(barniz.getId(), bodegaId,
                SolicitudDeAjusteRapido.Operacion.SUMAR, new BigDecimal("2"), TipoAjuste.CONTEO_FISICO, "Apareció en bodega"));

        assertThat(movimiento().tipo()).isEqualTo(TipoMovimiento.ENTRADA_AJUSTE);
    }

    @Test
    @DisplayName("HU-159 C4 · Un ajuste no toca el stock mínimo ni el máximo del producto")
    void noTocaMinimoNiMaximo() {
        hay("8");

        gestion.ajusteRapido(new SolicitudDeAjusteRapido(barniz.getId(), bodegaId,
                SolicitudDeAjusteRapido.Operacion.RESTAR, new BigDecimal("1"), TipoAjuste.MERMA, "Merma"));

        assertThat(barniz.getStockMinimo()).isEqualByComparingTo("2");
        assertThat(barniz.getStockMaximo()).isEqualByComparingTo("20");
        verify(productos, never()).save(any());
    }
}
