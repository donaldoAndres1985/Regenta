package com.regenta.compras.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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

import com.regenta.compras.domain.ConfiguracionDeCompras;
import com.regenta.compras.domain.EstadoOrdenCompra;
import com.regenta.compras.domain.OrdenCompraLinea;
import com.regenta.compras.domain.OrdenDeCompra;
import com.regenta.compras.infrastructure.AsignadorDeConsecutivos;
import com.regenta.compras.infrastructure.ConfiguracionDeComprasRepositorio;
import com.regenta.compras.infrastructure.OrdenCompraLineaRepositorio;
import com.regenta.compras.infrastructure.OrdenDeCompraRepositorio;
import com.regenta.compras.infrastructure.ProveedorProductoRepositorio;
import com.regenta.compras.infrastructure.ProveedorRepositorio;
import com.regenta.shared.events.RegistroDeEventos;
import com.regenta.shared.tenancy.ContextoDeNegocio;
import com.regenta.shared.tenancy.DatosDelNegocio;
import com.regenta.shared.web.ConflictoDeEstadoException;
import com.regenta.shared.web.ReglaDeNegocioException;

/** HU-173 · Aprobación y tolerancia configurables (I9, I15, I33). */
class AprobacionYToleranciaTest {

    private final OrdenDeCompraRepositorio ordenes = mock(OrdenDeCompraRepositorio.class);
    private final OrdenCompraLineaRepositorio lineas = mock(OrdenCompraLineaRepositorio.class);
    private final ConfiguracionDeComprasRepositorio configuraciones = mock(ConfiguracionDeComprasRepositorio.class);
    private final RegistroDeEventos eventos = mock(RegistroDeEventos.class);
    private final GestionDeConfiguracionDeCompras configuracion = new GestionDeConfiguracionDeCompras(configuraciones);
    private final GestionDeOrdenesDeCompra gestion = new GestionDeOrdenesDeCompra(ordenes, lineas,
            mock(ProveedorRepositorio.class), mock(ProveedorProductoRepositorio.class),
            mock(AsignadorDeConsecutivos.class), eventos, configuracion,
            mock(com.regenta.inventario.api.InventarioApi.class), null);

    private final UUID negocioId = UUID.randomUUID();
    private final UUID usuarioId = UUID.randomUUID();

    @BeforeEach
    void contexto() {
        ContextoDeNegocio.establecer(new DatosDelNegocio(negocioId, usuarioId, "PROFESIONAL", "VENTA_DIRECTA",
                Set.of(), Set.of("COMPRAS"), Set.of(), Set.of()));
        when(configuraciones.findById(negocioId)).thenReturn(Optional.empty());
        when(configuraciones.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    @AfterEach
    void limpiar() {
        ContextoDeNegocio.limpiar();
    }

    /** Una orden en borrador por {@code total}: una línea de 1 und a ese costo, sin IVA. */
    private OrdenDeCompra orden(String total) {
        OrdenDeCompra o = OrdenDeCompra.crear(negocioId, "OC-1", UUID.randomUUID(), UUID.randomUUID(), null,
                usuarioId, null, BigDecimal.ZERO, null);
        OrdenCompraLinea l = OrdenCompraLinea.nueva(o.getId(), negocioId, 1, UUID.randomUUID(), "Cemento",
                BigDecimal.ONE, new BigDecimal(total), BigDecimal.ZERO, BigDecimal.ZERO);
        o.totalizar(List.of(l));
        when(ordenes.findByIdAndNegocioId(o.getId(), negocioId)).thenReturn(Optional.of(o));
        when(lineas.existsByOrdenId(o.getId())).thenReturn(true);
        when(lineas.findByOrdenIdOrderByLinea(o.getId())).thenReturn(List.of(l));
        return o;
    }

    private void configurar(boolean exige, String minimo, String tolerancia) {
        ConfiguracionDeCompras c = ConfiguracionDeCompras.porOmision(negocioId);
        c.cambiar(exige, minimo == null ? null : new BigDecimal(minimo), new BigDecimal(tolerancia));
        when(configuraciones.findById(negocioId)).thenReturn(Optional.of(c));
    }

    @Test
    @DisplayName("HU-173 C1 · Un negocio nuevo exige aprobación, sin monto mínimo, y su tolerancia es 5 %")
    void porOmision() {
        ConfiguracionDeComprasDelNegocio c = configuracion.ver();

        assertThat(c.exigeAprobacion()).isTrue();
        assertThat(c.montoMinimoAprobacion()).isNull();
        assertThat(c.toleranciaRecepcionPct()).isEqualByComparingTo("5");
    }

    @Test
    @DisplayName("HU-173 C1 · Con la configuración por omisión, confirmar deja la orden esperando aprobación")
    void porOmisionEspera() {
        OrdenDeCompra o = orden("100000");

        OrdenDelNegocio r = gestion.confirmar(o.getId());

        assertThat(r.estado()).isEqualTo("BORRADOR");
        assertThat(r.esperaAprobacion()).isTrue();
        verify(eventos, never()).registrar(any(), any(), any(), eq("orden_compra_aprobada"), any());
    }

    @Test
    @DisplayName("HU-173 C2 · Sin exigir aprobación, confirmar la deja APROBADA, automática y a mi nombre")
    void sinAprobacion() {
        configurar(false, null, "5");
        OrdenDeCompra o = orden("5000000");

        OrdenDelNegocio r = gestion.confirmar(o.getId());

        assertThat(r.estado()).isEqualTo("APROBADA");
        assertThat(r.aprobacionAutomatica()).isTrue();
        assertThat(r.aprobadoPor()).isEqualTo(usuarioId);
        assertThat(r.aprobadoEn()).isNotNull();
        verify(eventos).registrar(eq(negocioId), eq("OrdenDeCompra"), eq(o.getId()), eq("orden_compra_aprobada"), any());
    }

    @Test
    @DisplayName("HU-173 C3 · Con monto mínimo de $ 1.000.000, una orden de $ 800.000 se aprueba sola")
    void debajoDelMinimo() {
        configurar(true, "1000000", "5");

        OrdenDelNegocio r = gestion.confirmar(orden("800000").getId());

        assertThat(r.estado()).isEqualTo("APROBADA");
        assertThat(r.aprobacionAutomatica()).isTrue();
    }

    @Test
    @DisplayName("HU-173 C3 · Con monto mínimo de $ 1.000.000, una de $ 1.200.000 queda esperando aprobación")
    void encimaDelMinimo() {
        configurar(true, "1000000", "5");
        OrdenDeCompra o = orden("1200000");

        OrdenDelNegocio r = gestion.confirmar(o.getId());

        assertThat(r.estado()).isEqualTo("BORRADOR");
        assertThat(r.esperaAprobacion()).isTrue();

        OrdenDelNegocio aprobada = gestion.aprobar(o.getId());
        assertThat(aprobada.estado()).isEqualTo("APROBADA");
        assertThat(aprobada.aprobacionAutomatica()).isFalse();
    }

    @Test
    @DisplayName("HU-173 · Una orden que ya no está en borrador no se confirma (409)")
    void yaAprobada() {
        configurar(false, null, "5");
        OrdenDeCompra o = orden("100000");
        gestion.confirmar(o.getId());

        assertThatThrownBy(() -> gestion.confirmar(o.getId())).isInstanceOf(ConflictoDeEstadoException.class);
    }

    @Test
    @DisplayName("HU-173 · Cambiar las líneas de una orden que esperaba aprobación obliga a confirmarla otra vez")
    void editarReiniciaLaEspera() {
        OrdenDeCompra o = orden("1200000");
        gestion.confirmar(o.getId());
        assertThat(o.esperaAprobacion()).isTrue();

        o.exigirBorradorParaEditar();

        assertThat(o.esperaAprobacion()).isFalse();
    }

    @Test
    @DisplayName("HU-173 C4 · Con tolerancia 0 %, recibir 101 de 100 se rechaza; con 10 %, se aceptan hasta 110")
    void tolerancia() {
        OrdenCompraLinea cero = OrdenCompraLinea.nueva(UUID.randomUUID(), negocioId, 1, UUID.randomUUID(), "Arena",
                new BigDecimal("100"), BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ZERO);
        assertThatThrownBy(() -> cero.recibir(new BigDecimal("101"), BigDecimal.ZERO))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("tolerancia de 0 %");

        OrdenCompraLinea diez = OrdenCompraLinea.nueva(UUID.randomUUID(), negocioId, 1, UUID.randomUUID(), "Arena",
                new BigDecimal("100"), BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ZERO);
        diez.recibir(new BigDecimal("110"), BigDecimal.TEN);
        assertThat(diez.getCantidadRecibida()).isEqualByComparingTo("110");
        assertThatThrownBy(() -> diez.recibir(new BigDecimal("1"), BigDecimal.TEN))
                .isInstanceOf(ReglaDeNegocioException.class);
    }

    @Test
    @DisplayName("HU-173 · Se guarda la configuración; tolerancia fuera de 0 a 100 o mínimo no positivo se rechazan")
    void cambiarConfiguracion() {
        ConfiguracionDeComprasDelNegocio c = configuracion.cambiar(
                new SolicitudDeConfiguracionDeCompras(true, new BigDecimal("1000000"), BigDecimal.ZERO));
        assertThat(c.montoMinimoAprobacion()).isEqualByComparingTo("1000000");
        assertThat(c.toleranciaRecepcionPct()).isEqualByComparingTo("0");

        assertThatThrownBy(() -> configuracion.cambiar(
                new SolicitudDeConfiguracionDeCompras(true, null, new BigDecimal("101"))))
                .isInstanceOf(ReglaDeNegocioException.class);
        assertThatThrownBy(() -> configuracion.cambiar(
                new SolicitudDeConfiguracionDeCompras(true, BigDecimal.ZERO, BigDecimal.TEN)))
                .isInstanceOf(ReglaDeNegocioException.class);
    }

    @Test
    @DisplayName("HU-173 · Sin exigir aprobación, el monto mínimo no se guarda")
    void sinExigirSinMinimo() {
        ConfiguracionDeComprasDelNegocio c = configuracion.cambiar(
                new SolicitudDeConfiguracionDeCompras(false, new BigDecimal("1000000"), BigDecimal.TEN));

        assertThat(c.exigeAprobacion()).isFalse();
        assertThat(c.montoMinimoAprobacion()).isNull();
        assertThat(EstadoOrdenCompra.values()).contains(EstadoOrdenCompra.APROBADA);
    }
}
