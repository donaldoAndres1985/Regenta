package com.regenta.alertas.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.regenta.alertas.domain.Alerta;
import com.regenta.alertas.domain.CanalDeAlerta;
import com.regenta.alertas.domain.Entrega;
import com.regenta.alertas.domain.EstadoEntrega;
import com.regenta.alertas.domain.Severidad;
import com.regenta.alertas.infrastructure.AlertaRepositorio;
import com.regenta.alertas.infrastructure.DispositivoPushRepositorio;
import com.regenta.alertas.infrastructure.EntregaRepositorio;
import com.regenta.alertas.infrastructure.PreferenciaNotificacionRepositorio;
import com.regenta.shared.correo.CorreoEnMemoria;
import com.regenta.shared.tenancy.ContextoDeNegocio;
import com.regenta.shared.tenancy.DatosDelNegocio;

/** HU-122 · Las alertas por correo salen por el puerto común; un rechazo entra al reintento de la entrega. */
class CorreoDeAlertasTest {

    private final UUID negocio = UUID.randomUUID();
    private final EntregaRepositorio entregas = mock(EntregaRepositorio.class);
    private final AlertaRepositorio alertas = mock(AlertaRepositorio.class);
    private final PreferenciaNotificacionRepositorio preferencias = mock(PreferenciaNotificacionRepositorio.class);
    private final CorreoEnMemoria correo = new CorreoEnMemoria();
    private final DespachoDeEntregas despacho = new DespachoDeEntregas(entregas, alertas,
            mock(DispositivoPushRepositorio.class), preferencias, mock(PasarelaDePush.class), correo);
    private final Alerta alerta = mock(Alerta.class);
    private Entrega entrega;

    @BeforeEach
    void preparar() {
        ContextoDeNegocio.establecer(new DatosDelNegocio(negocio, null, "", "", Set.of("SISTEMA"), Set.of("ALERTAS"),
                Set.of("ALERTAS_ALERTA_EDITAR"), Set.of()));
        UUID alertaId = UUID.randomUUID();
        when(alerta.getId()).thenReturn(alertaId);
        when(alerta.getNegocioId()).thenReturn(negocio);
        when(alerta.getTitulo()).thenReturn("Stock bajo: cemento gris");
        when(alerta.getMensaje()).thenReturn("Quedan 3 bultos en la bodega principal");
        when(alerta.getTipoCodigo()).thenReturn("STOCK_BAJO");
        when(alerta.getSeveridad()).thenReturn(Severidad.ALTA);
        when(alertas.findById(alertaId)).thenReturn(Optional.of(alerta));
        when(preferencias.findByUsuarioIdAndTipoCodigo(any(), any())).thenReturn(Optional.empty());
        entrega = Entrega.pendiente(negocio, alertaId, UUID.randomUUID(), CanalDeAlerta.EMAIL, "admin@negocio.co");
        when(entregas.paraReintentar(any(), any())).thenReturn(List.of(entrega));
    }

    @AfterEach
    void limpiar() {
        ContextoDeNegocio.limpiar();
    }

    @Test
    @DisplayName("C1 · Sale por el correo común, del negocio de la alerta, y la entrega guarda el id del proveedor")
    void sale() {
        despacho.reintentarPendientes();

        assertThat(correo.enviados()).singleElement().satisfies(c -> {
            assertThat(c.negocioId()).isEqualTo(negocio);
            assertThat(c.para()).isEqualTo("admin@negocio.co");
            assertThat(c.asunto()).isEqualTo("Stock bajo: cemento gris");
        });
        assertThat(entrega.getEstado()).isEqualTo(EstadoEntrega.ENVIADA);
        assertThat(entrega.getProveedorId()).startsWith("<memoria-");
    }

    @Test
    @DisplayName("C2 · Un rechazo deja la entrega FALLIDA con el motivo y su próximo reintento; al quinto, sin más")
    void rechazo() {
        correo.fallarCon("550 5.1.1 Mailbox does not exist");

        despacho.reintentarPendientes();
        assertThat(entrega.getError()).isEqualTo("550 5.1.1 Mailbox does not exist");
        assertThat(entrega.getIntentos()).isEqualTo(1);
        assertThat(entrega.getEstado()).isEqualTo(EstadoEntrega.FALLIDA);
        assertThat(entrega.getProximoIntento()).isNotNull();

        for (int i = 0; i < 4; i++) {
            despacho.reintentarPendientes();
        }
        assertThat(entrega.getIntentos()).isEqualTo(5);
        assertThat(entrega.getEstado()).isEqualTo(EstadoEntrega.FALLIDA);
        assertThat(entrega.getProximoIntento()).isNull();
    }
}
