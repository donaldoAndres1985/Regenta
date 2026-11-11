package com.regenta.ventas.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.regenta.shared.tenancy.ContextoDeNegocio;
import com.regenta.shared.tenancy.DatosDelNegocio;
import com.regenta.ventas.domain.EstadoVenta;
import com.regenta.ventas.domain.Venta;
import com.regenta.ventas.infrastructure.AsignadorDeConsecutivos;
import com.regenta.ventas.infrastructure.VentaLineaRepositorio;
import com.regenta.ventas.infrastructure.VentaRepositorio;

/**
 * Etapa 0 del cobro real: la nota de quien vende al crear el borrador, y el
 * listado de borradores ("ventas en espera") que el POS recompone al abrir.
 */
class GestionDeVentasTest {

    private final VentaRepositorio ventas = mock(VentaRepositorio.class);
    private final VentaLineaRepositorio lineas = mock(VentaLineaRepositorio.class);
    private final AsignadorDeConsecutivos consecutivos = mock(AsignadorDeConsecutivos.class);
    private final SagaDeConfirmacionDeVenta saga = mock(SagaDeConfirmacionDeVenta.class);
    private final ConfiguracionDelNegocio configuracion = mock(ConfiguracionDelNegocio.class);

    private final GestionDeVentas gestion =
            new GestionDeVentas(ventas, lineas, consecutivos, saga, configuracion);

    private final UUID negocioId = UUID.randomUUID();
    private final UUID usuarioId = UUID.randomUUID();
    private final UUID bodegaId = UUID.randomUUID();

    @BeforeEach
    void contexto() {
        ContextoDeNegocio.establecer(new DatosDelNegocio(negocioId, usuarioId, "BASICO",
                "VENTA_DIRECTA", Set.of(), Set.of(), Set.of("VENTAS_VENTA_CREAR", "VENTAS_VENTA_VER"),
                Set.of()));
        when(consecutivos.siguiente(any(), any())).thenReturn(1L);
        when(lineas.findByVentaIdOrderByLinea(any())).thenReturn(List.of());
    }

    @AfterEach
    void limpiar() {
        ContextoDeNegocio.limpiar();
    }

    @Test
    void crearBorradorGuardaLaNotaDeQuienVende() {
        VentaDelNegocio dto = gestion.crearBorrador(
                new SolicitudDeVenta(bodegaId, null, null, null, "Entregar en obra"));

        assertThat(dto.notaCliente()).isEqualTo("Entregar en obra");

        ArgumentCaptor<Venta> guardada = ArgumentCaptor.forClass(Venta.class);
        verify(ventas).save(guardada.capture());
        assertThat(guardada.getValue().getNotaCliente()).isEqualTo("Entregar en obra");
    }

    @Test
    void crearBorradorSinNotaNoFallaYQuedaEnNull() {
        VentaDelNegocio dto = gestion.crearBorrador(new SolicitudDeVenta(bodegaId, null, null, null, null));

        assertThat(dto.notaCliente()).isNull();
    }

    @Test
    void listarBorradoresAbiertosDelegaConElUsuarioYLaBodegaDeLaPeticion() {
        Venta borrador = Venta.crear(negocioId, bodegaId, "7", null, usuarioId, null, "MOSTRADOR");
        when(ventas.findByNegocioIdAndUsuarioIdAndBodegaIdAndEstadoOrderByCreadoEnDesc(negocioId,
                usuarioId, bodegaId, EstadoVenta.BORRADOR)).thenReturn(List.of(borrador));

        List<VentaDelNegocio> borradores = gestion.listarBorradoresAbiertos(bodegaId);

        assertThat(borradores).hasSize(1);
        assertThat(borradores.get(0).numero()).isEqualTo("7");
        assertThat(borradores.get(0).estado()).isEqualTo(EstadoVenta.BORRADOR);
    }
}
