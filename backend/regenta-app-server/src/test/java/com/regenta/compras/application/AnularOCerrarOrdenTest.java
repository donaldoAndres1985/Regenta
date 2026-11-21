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
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

import com.regenta.compras.domain.EstadoOrdenCompra;
import com.regenta.compras.domain.EstadoRecepcion;
import com.regenta.compras.domain.OrdenCompraLinea;
import com.regenta.compras.domain.OrdenDeCompra;
import com.regenta.compras.domain.Recepcion;
import com.regenta.compras.infrastructure.OrdenDeCompraRepositorio;
import com.regenta.compras.infrastructure.RecepcionRepositorio;
import com.regenta.shared.events.RegistroDeEventos;
import com.regenta.shared.security.GuardiaDePermisos;
import com.regenta.shared.tenancy.ContextoDeNegocio;
import com.regenta.shared.tenancy.DatosDelNegocio;
import com.regenta.shared.web.ConflictoDeEstadoException;
import com.regenta.shared.web.ReglaDeNegocioException;
import com.regenta.shared.web.SinPermisoException;

/** HU-176 · Anular una orden que no va, o cerrarla cuando llegó incompleta (I7, I11, I12). */
class AnularOCerrarOrdenTest {

    private final OrdenDeCompraRepositorio ordenes = mock(OrdenDeCompraRepositorio.class);
    private final RecepcionRepositorio recepciones = mock(RecepcionRepositorio.class);
    private final RegistroDeEventos eventos = mock(RegistroDeEventos.class);
    private final GestionDeOrdenesDeCompra gestion = mock(GestionDeOrdenesDeCompra.class);
    private final FinDeOrdenesDeCompra fin = new FinDeOrdenesDeCompra(ordenes, recepciones, eventos, gestion);

    private final UUID negocioId = UUID.randomUUID();
    private final UUID usuarioId = UUID.randomUUID();
    private OrdenDeCompra orden;
    private OrdenCompraLinea linea;

    @BeforeEach
    void datos() {
        conPermisos("COMPRAS_COMPRA_ANULAR", "COMPRAS_COMPRA_VER");
        orden = OrdenDeCompra.crear(negocioId, "OC-1", UUID.randomUUID(), UUID.randomUUID(), null, usuarioId, null,
                BigDecimal.ZERO, null);
        linea = OrdenCompraLinea.nueva(orden.getId(), negocioId, 1, UUID.randomUUID(), "Cemento",
                new BigDecimal("100"), new BigDecimal("30000"), BigDecimal.ZERO, BigDecimal.ZERO);
        when(ordenes.findByIdAndNegocioId(orden.getId(), negocioId)).thenReturn(Optional.of(orden));
        when(recepciones.findByNegocioIdAndOrdenIdOrderByFechaDesc(negocioId, orden.getId())).thenReturn(List.of());
        when(gestion.vistaDe(any())).thenAnswer(i -> OrdenDelNegocio.de(i.getArgument(0), List.of(linea)));
    }

    @AfterEach
    void limpiar() {
        ContextoDeNegocio.limpiar();
    }

    private void conPermisos(String... permisos) {
        ContextoDeNegocio.establecer(new DatosDelNegocio(negocioId, usuarioId, "PROFESIONAL", "VENTA_DIRECTA",
                Set.of(permisos), Set.of("COMPRAS"), Set.of(), Set.of()));
    }

    private void llevarA(String estado) {
        if (estado.equals("BORRADOR")) {
            return;
        }
        orden.aprobar(UUID.randomUUID(), true);
        if (estado.equals("ENVIADA")) {
            orden.enviar();
        }
    }

    private void parcialCon60() {
        llevarA("ENVIADA");
        linea.recibir(new BigDecimal("60"), new BigDecimal("5"));
        orden.registrarAvanceDeRecepcion(List.of(linea));
    }

    private Recepcion recepcion(boolean confirmada) {
        Recepcion r = Recepcion.crear(negocioId, "REC-1", orden.getId(), orden.getProveedorId(),
                orden.getBodegaDestinoId(), null, usuarioId, null);
        if (confirmada) {
            r.confirmar();
        }
        return r;
    }

    @ParameterizedTest(name = "C1 · Una orden {0} sin recepciones se anula con motivo, usuario y fecha")
    @ValueSource(strings = {"BORRADOR", "APROBADA", "ENVIADA"})
    void anular(String estado) {
        llevarA(estado);

        OrdenDelNegocio vista = fin.anular(orden.getId(), new SolicitudDeFinDeOrden("  El proveedor no tiene  "));

        assertThat(orden.getEstado()).isEqualTo(EstadoOrdenCompra.ANULADA);
        assertThat(vista.motivoAnulacion()).isEqualTo("El proveedor no tiene");
        assertThat(vista.anuladaPor()).isEqualTo(usuarioId);
        assertThat(vista.anuladaEn()).isNotNull();
        verify(ordenes).save(orden);
        ArgumentCaptor<Object> datos = ArgumentCaptor.forClass(Object.class);
        verify(eventos).registrar(eq(negocioId), eq("OrdenDeCompra"), eq(orden.getId()), eq("orden_compra_anulada"),
                datos.capture());
        assertThat(((Map<?, ?>) datos.getValue()).get("motivo")).isEqualTo("El proveedor no tiene");
    }

    @Test
    @DisplayName("C1 · La recepción que se estaba armando para la orden se descarta")
    void descartaElBorrador() {
        llevarA("ENVIADA");
        Recepcion borrador = recepcion(false);
        when(recepciones.findByNegocioIdAndOrdenIdOrderByFechaDesc(negocioId, orden.getId()))
                .thenReturn(List.of(borrador));

        fin.anular(orden.getId(), new SolicitudDeFinDeOrden("No va"));

        assertThat(borrador.getEstado()).isEqualTo(EstadoRecepcion.ANULADA);
        verify(recepciones).save(borrador);
    }

    @Test
    @DisplayName("C2 · Con una recepción confirmada no se anula: 409")
    void conRecepcionConfirmada() {
        llevarA("ENVIADA");
        when(recepciones.findByNegocioIdAndOrdenIdOrderByFechaDesc(negocioId, orden.getId()))
                .thenReturn(List.of(recepcion(true)));

        assertThatThrownBy(() -> fin.anular(orden.getId(), new SolicitudDeFinDeOrden("No va")))
                .isInstanceOf(ConflictoDeEstadoException.class)
                .hasMessageContaining("se cierra con faltante");
        assertThat(orden.getEstado()).isEqualTo(EstadoOrdenCompra.ENVIADA);
        verify(ordenes, never()).save(any());
    }

    @Test
    @DisplayName("C2 · Una orden PARCIAL tampoco se anula: 409")
    void parcialNoSeAnula() {
        parcialCon60();

        assertThatThrownBy(() -> fin.anular(orden.getId(), new SolicitudDeFinDeOrden("No va")))
                .isInstanceOf(ConflictoDeEstadoException.class);
    }

    @Test
    @DisplayName("C1 · Sin motivo no se anula: 422")
    void sinMotivo() {
        assertThatThrownBy(() -> fin.anular(orden.getId(), new SolicitudDeFinDeOrden("   ")))
                .isInstanceOf(ReglaDeNegocioException.class);
        assertThat(orden.getEstado()).isEqualTo(EstadoOrdenCompra.BORRADOR);
    }

    @Test
    @DisplayName("C3 · Una PARCIAL con 40 de faltante se cierra: CERRADA, sin más recepciones y el faltante a la vista")
    void cerrar() {
        parcialCon60();

        OrdenDelNegocio vista = fin.cerrar(orden.getId(), new SolicitudDeFinDeOrden("El resto no llegará"));

        assertThat(vista.estado()).isEqualTo("CERRADA");
        assertThat(vista.motivoCierre()).isEqualTo("El resto no llegará");
        assertThat(vista.cerradaPor()).isEqualTo(usuarioId);
        assertThat(vista.lineas().get(0).faltante()).isEqualByComparingTo("40");
        assertThatThrownBy(orden::exigirRecibible).isInstanceOf(ConflictoDeEstadoException.class);
        verify(eventos).registrar(eq(negocioId), eq("OrdenDeCompra"), eq(orden.getId()), eq("orden_compra_cerrada"),
                any());
    }

    @Test
    @DisplayName("C3 · Una orden sin nada recibido no se cierra: se anula (409)")
    void cerrarSinRecibir() {
        llevarA("ENVIADA");

        assertThatThrownBy(() -> fin.cerrar(orden.getId(), new SolicitudDeFinDeOrden("No va")))
                .isInstanceOf(ConflictoDeEstadoException.class)
                .hasMessageContaining("anúlala");
    }

    @Test
    @DisplayName("C4 · Sin COMPRAS_COMPRA_ANULAR, anular y cerrar responden 403")
    void sinPermiso() {
        parcialCon60();
        conPermisos("COMPRAS_COMPRA_VER", "COMPRAS_COMPRA_CREAR", "COMPRAS_COMPRA_APROBAR");
        AspectJProxyFactory fabrica = new AspectJProxyFactory(fin);
        fabrica.setProxyTargetClass(true);
        fabrica.addAspect(new GuardiaDePermisos());
        FinDeOrdenesDeCompra conGuardia = fabrica.getProxy();

        assertThatThrownBy(() -> conGuardia.anular(orden.getId(), new SolicitudDeFinDeOrden("No va")))
                .isInstanceOf(SinPermisoException.class);
        assertThatThrownBy(() -> conGuardia.cerrar(orden.getId(), new SolicitudDeFinDeOrden("No llega")))
                .isInstanceOf(SinPermisoException.class);
        assertThat(orden.getEstado()).isEqualTo(EstadoOrdenCompra.PARCIAL);
    }
}
