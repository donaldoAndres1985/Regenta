package com.regenta.alertas.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.regenta.alertas.domain.Alerta;
import com.regenta.alertas.domain.CanalDeAlerta;
import com.regenta.alertas.domain.DispositivoPush;
import com.regenta.alertas.domain.Entrega;
import com.regenta.alertas.domain.EstadoEntrega;
import com.regenta.alertas.domain.PlataformaDispositivo;
import com.regenta.alertas.domain.Severidad;
import com.regenta.alertas.infrastructure.AlertaRepositorio;
import com.regenta.alertas.infrastructure.DispositivoPushRepositorio;
import com.regenta.alertas.infrastructure.EntregaRepositorio;
import com.regenta.alertas.infrastructure.FcmSimulado;
import com.regenta.alertas.infrastructure.PasarelaFcm;
import com.regenta.alertas.infrastructure.PreferenciaNotificacionRepositorio;
import com.regenta.shared.correo.CorreoEnMemoria;
import com.regenta.shared.tenancy.ContextoDeNegocio;
import com.regenta.shared.tenancy.DatosDelNegocio;

/** HU-123 · Una alerta con canal push llega al celular por FCM (simulado por contrato). */
class PushDeAlertasTest {

    private final UUID negocio = UUID.randomUUID();
    private final UUID usuario = UUID.randomUUID();
    private final EntregaRepositorio entregas = mock(EntregaRepositorio.class);
    private final AlertaRepositorio alertas = mock(AlertaRepositorio.class);
    private final DispositivoPushRepositorio dispositivos = mock(DispositivoPushRepositorio.class);
    private final PreferenciaNotificacionRepositorio preferencias = mock(PreferenciaNotificacionRepositorio.class);
    private final Alerta alerta = mock(Alerta.class);
    private final List<DispositivoPush> suyos = new ArrayList<>();
    private FcmSimulado fcm;
    private DespachoDeEntregas despacho;
    private Entrega entrega;

    @BeforeEach
    void preparar() throws Exception {
        fcm = new FcmSimulado();
        despacho = new DespachoDeEntregas(entregas, alertas, dispositivos, preferencias,
                new PasarelaFcm(new ObjectMapper(), fcm.credenciales(), fcm.url()), new CorreoEnMemoria());
        ContextoDeNegocio.establecer(new DatosDelNegocio(negocio, null, "", "", Set.of("SISTEMA"), Set.of("ALERTAS"),
                Set.of(), Set.of()));
        UUID alertaId = UUID.randomUUID();
        when(alerta.getId()).thenReturn(alertaId);
        when(alerta.getNegocioId()).thenReturn(negocio);
        when(alerta.getTitulo()).thenReturn("Stock bajo: cemento gris");
        when(alerta.getMensaje()).thenReturn("Quedan 3 bultos en la bodega principal");
        when(alerta.getTipoCodigo()).thenReturn("STOCK_MINIMO");
        when(alerta.getSeveridad()).thenReturn(Severidad.ALTA);
        when(alerta.getRutaApp()).thenReturn("/inventario/productos/42");
        when(alertas.findById(alertaId)).thenReturn(Optional.of(alerta));
        when(preferencias.findByUsuarioIdAndTipoCodigo(any(), any())).thenReturn(Optional.empty());
        when(dispositivos.findByNegocioIdAndUsuarioIdAndActivoTrue(negocio, usuario))
                .thenAnswer(i -> suyos.stream().filter(DispositivoPush::isActivo).toList());
        entrega = Entrega.pendiente(negocio, alertaId, usuario, CanalDeAlerta.PUSH, null);
        when(entregas.findByAlertaId(alertaId)).thenReturn(List.of(entrega));
    }

    @AfterEach
    void limpiar() {
        fcm.close();
        ContextoDeNegocio.limpiar();
    }

    private DispositivoPush celular(String token, PlataformaDispositivo plataforma) {
        DispositivoPush d = DispositivoPush.registrar(negocio, usuario, token, plataforma, "Pixel", "1.0.0");
        suyos.add(d);
        return d;
    }

    @Test
    void c2_llegaAlDispositivoYLaEntregaQuedaConElIdDeFcm() {
        celular("token-android", PlataformaDispositivo.ANDROID);

        despacho.despacharAlerta(alerta.getId());

        assertThat(fcm.envios).singleElement().satisfies(e -> {
            assertThat(e.token()).isEqualTo("token-android");
            assertThat(e.titulo()).isEqualTo("Stock bajo: cemento gris");
            assertThat(e.datos()).containsEntry("ruta", "/inventario/productos/42");
        });
        assertThat(entrega.getEstado()).isEqualTo(EstadoEntrega.ENVIADA);
        assertThat(entrega.getProveedorId()).startsWith("0:17000");
    }

    @Test
    void c4_conVariosDispositivosLlegaATodosLosActivos() {
        celular("celular-1", PlataformaDispositivo.ANDROID);
        celular("celular-2", PlataformaDispositivo.ANDROID);
        DispositivoPush apagado = celular("viejo", PlataformaDispositivo.ANDROID);
        apagado.desactivar();

        despacho.despacharAlerta(alerta.getId());

        assertThat(fcm.envios).extracting(FcmSimulado.Envio::token).containsExactlyInAnyOrder("celular-1", "celular-2");
        assertThat(entrega.getEstado()).isEqualTo(EstadoEntrega.ENVIADA);
        assertThat(entrega.getProveedorId().split(",")).hasSize(2);
    }

    @Test
    void c3_unTokenQueFcmRechazaDejaElDispositivoInactivoYNoSeLeVuelveAEnviar() {
        DispositivoPush viejo = celular("token-viejo", PlataformaDispositivo.ANDROID);
        celular("token-nuevo", PlataformaDispositivo.ANDROID);
        fcm.responder("token-viejo", 404, FcmSimulado.noRegistrado());

        despacho.despacharAlerta(alerta.getId());

        assertThat(viejo.isActivo()).isFalse();
        ArgumentCaptor<DispositivoPush> guardado = ArgumentCaptor.forClass(DispositivoPush.class);
        verify(dispositivos).save(guardado.capture());
        assertThat(guardado.getValue().getTokenFcm()).isEqualTo("token-viejo");
        assertThat(entrega.getEstado()).isEqualTo(EstadoEntrega.ENVIADA);

        // La próxima alerta ya no va al token muerto.
        fcm.envios.clear();
        Entrega otra = Entrega.pendiente(negocio, alerta.getId(), usuario, CanalDeAlerta.PUSH, null);
        when(entregas.findByAlertaId(alerta.getId())).thenReturn(List.of(otra));
        despacho.despacharAlerta(alerta.getId());
        assertThat(fcm.envios).extracting(FcmSimulado.Envio::token).containsExactly("token-nuevo");
    }

    @Test
    void c5_quienSoloUsaLaWebNoTieneUnaEntregaFallidaSinoNoSoportada() {
        celular("token-web", PlataformaDispositivo.WEB);

        despacho.despacharAlerta(alerta.getId());

        assertThat(fcm.envios).isEmpty();
        assertThat(entrega.getEstado()).isEqualTo(EstadoEntrega.NO_SOPORTADA);
        assertThat(entrega.getProximoIntento()).isNull();
        assertThat(entrega.getError()).contains("Web");
    }

    @Test
    void c5_sinNingunDispositivoTampocoEsUnFallo() {
        despacho.despacharAlerta(alerta.getId());

        assertThat(entrega.getEstado()).isEqualTo(EstadoEntrega.NO_SOPORTADA);
        assertThat(entrega.estaPendienteDeEnvio()).isFalse();
    }

    @Test
    void siFcmEstaCaidoLaEntregaEntraAlReintento() {
        celular("token-android", PlataformaDispositivo.ANDROID);
        fcm.responder("token-android", 503, FcmSimulado.noDisponible());

        despacho.despacharAlerta(alerta.getId());

        assertThat(entrega.getEstado()).isEqualTo(EstadoEntrega.FALLIDA);
        assertThat(entrega.getProximoIntento()).isNotNull();
        assertThat(suyos.get(0).isActivo()).isTrue();
    }
}
