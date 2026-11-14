package com.regenta.caja.application;

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
import org.mockito.ArgumentCaptor;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

import com.regenta.caja.domain.MovimientoDeCaja;
import com.regenta.caja.domain.SesionDeCaja;
import com.regenta.caja.domain.TipoMovimientoCaja;
import com.regenta.caja.infrastructure.ConfigDeCajaRepositorio;
import com.regenta.caja.infrastructure.MovimientoDeCajaRepositorio;
import com.regenta.caja.infrastructure.SesionDeCajaRepositorio;
import com.regenta.shared.events.RegistroDeEventos;
import com.regenta.shared.security.GuardiaDePermisos;
import com.regenta.shared.tenancy.ContextoDeNegocio;
import com.regenta.shared.tenancy.DatosDelNegocio;
import com.regenta.shared.web.ConflictoDeEstadoException;
import com.regenta.shared.web.NoEncontradoException;
import com.regenta.shared.web.SinPermisoException;

/** HU-141 · anular un movimiento manual, en el caso de uso (con la guardia de permisos real). */
class AnularMovimientoTest {

    private final MovimientoDeCajaRepositorio movimientos = mock(MovimientoDeCajaRepositorio.class);
    private final SesionDeCajaRepositorio sesiones = mock(SesionDeCajaRepositorio.class);
    private final RegistroDeEventos eventos = mock(RegistroDeEventos.class);
    private final GestionDeConfigDeCaja config =
            new GestionDeConfigDeCaja(mock(ConfigDeCajaRepositorio.class));

    private final UUID negocioId = UUID.randomUUID();
    private SesionDeCaja sesion;
    private MovimientoDeCaja retiro;

    @BeforeEach
    void preparar() {
        sesion = SesionDeCaja.abrir(negocioId, null, UUID.randomUUID(), "CAJA-3", UUID.randomUUID(),
                BigDecimal.ZERO);
        retiro = MovimientoDeCaja.manual(negocioId, sesion.getId(), UUID.randomUUID(),
                TipoMovimientoCaja.RETIRO, "Consignación", new BigDecimal("40000"), null, "k-1");
        when(sesiones.findByIdAndNegocioId(sesion.getId(), negocioId)).thenReturn(Optional.of(sesion));
        when(movimientos.findByIdAndNegocioId(retiro.getId(), negocioId)).thenReturn(Optional.of(retiro));
        when(movimientos.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @AfterEach
    void limpiar() {
        ContextoDeNegocio.limpiar();
    }

    @Test
    @DisplayName("HU-141 C1 · Dado un usuario con CAJA_MOVIMIENTO_ANULAR, cuando anula con motivo, se registra la anulación")
    void conPermisoSeAnula() {
        conPermisos("CAJA_MOVIMIENTO_ANULAR");

        MovimientoDelNegocio anulacion = servicio().anular(sesion.getId(), retiro.getId(),
                new SolicitudDeAnulacion("Se registró dos veces"));

        assertThat(anulacion.tipo()).isEqualTo("ANULACION");
        assertThat(anulacion.anulaA()).isEqualTo(retiro.getId());
        verify(movimientos).save(any());
    }

    @Test
    @DisplayName("HU-141 C2 · Dado un usuario sin CAJA_MOVIMIENTO_ANULAR, cuando intenta anular, recibe 403")
    void sinPermisoSeRechaza() {
        conPermisos("CAJA_MOVIMIENTO_CREAR");

        assertThatThrownBy(() -> servicio().anular(sesion.getId(), retiro.getId(),
                new SolicitudDeAnulacion("Error")))
                .isInstanceOf(SinPermisoException.class);
        verify(movimientos, never()).save(any());
    }

    @Test
    @DisplayName("HU-141 C5 · Dado un movimiento ya anulado, cuando intento anularlo otra vez, recibe 409")
    void unMovimientoSeAnulaUnaSolaVez() {
        conPermisos("CAJA_MOVIMIENTO_ANULAR");
        when(movimientos.existsByAnulaA(retiro.getId())).thenReturn(true);

        assertThatThrownBy(() -> servicio().anular(sesion.getId(), retiro.getId(),
                new SolicitudDeAnulacion("Otra vez")))
                .isInstanceOf(ConflictoDeEstadoException.class);
        verify(movimientos, never()).save(any());
    }

    @Test
    @DisplayName("HU-141 C6 · Dada una sesión cerrada, cuando intento anular uno de sus movimientos, recibe 409")
    void enUnaSesionCerradaNoSeAnula() {
        conPermisos("CAJA_MOVIMIENTO_ANULAR");
        sesion.cerrarConArqueo(UUID.randomUUID(), List.of(new com.regenta.caja.domain.LineaDeArqueo(
                com.regenta.caja.domain.MetodoPagoCaja.EFECTIVO, true, BigDecimal.ONE, BigDecimal.ONE, 1)),
                null);

        assertThatThrownBy(() -> servicio().anular(sesion.getId(), retiro.getId(),
                new SolicitudDeAnulacion("Tarde")))
                .isInstanceOf(ConflictoDeEstadoException.class);
    }

    @Test
    @DisplayName("HU-141 C8 · Dada una anulación, se publica movimiento_de_caja_anulado con el movimiento, el usuario y el motivo")
    @SuppressWarnings("unchecked")
    void laAnulacionSePublica() {
        UUID supervisor = conPermisos("CAJA_MOVIMIENTO_ANULAR");

        servicio().anular(sesion.getId(), retiro.getId(), new SolicitudDeAnulacion("Duplicado"));

        ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
        verify(eventos).registrar(eq(negocioId), eq("MovimientoDeCaja"), any(),
                eq("movimiento_de_caja_anulado"), payload.capture());
        Map<String, Object> datos = (Map<String, Object>) payload.getValue();
        assertThat(datos).containsEntry("movimiento_anulado_id", retiro.getId().toString())
                .containsEntry("usuario_id", supervisor.toString())
                .containsEntry("motivo", "Duplicado");
    }

    @Test
    @DisplayName("HU-141 C9 · Dado un movimiento de otro negocio, cuando intento anularlo, recibe 404")
    void unMovimientoDeOtroNegocioNoExiste() {
        conPermisos("CAJA_MOVIMIENTO_ANULAR");
        UUID ajeno = UUID.randomUUID();
        when(movimientos.findByIdAndNegocioId(ajeno, negocioId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio().anular(sesion.getId(), ajeno,
                new SolicitudDeAnulacion("Error")))
                .isInstanceOf(NoEncontradoException.class);
    }

    private GestionDeMovimientosManuales servicio() {
        AspectJProxyFactory fabrica = new AspectJProxyFactory(
                new GestionDeMovimientosManuales(movimientos, sesiones, config, eventos));
        fabrica.setProxyTargetClass(true);
        fabrica.addAspect(new GuardiaDePermisos());
        return fabrica.getProxy();
    }

    private UUID conPermisos(String... permisos) {
        UUID usuario = UUID.randomUUID();
        ContextoDeNegocio.establecer(new DatosDelNegocio(negocioId, usuario, "EMPRESARIAL",
                "VENTA_DIRECTA", Set.of(), Set.of(), Set.of(permisos), Set.of()));
        return usuario;
    }
}
