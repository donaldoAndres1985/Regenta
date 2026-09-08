package com.regenta.alertas.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.regenta.alertas.BaseDeAlertas;
import com.regenta.alertas.aplicacion.PasarelaDePush.ResultadoDePush;
import com.regenta.alertas.infra.PasarelaDeCorreoStub;
import com.regenta.alertas.infra.PasarelaDePushStub;

/** HU-094. Entrega por push, correo y dentro de la app. */
class EntregaDeAlertasTest extends BaseDeAlertas {

    private static final Set<String> USUARIO = Set.of("ALERTAS_ALERTA_VER");
    private static final Set<String> ADMIN =
            Set.of("ALERTAS_ALERTA_VER", "ALERTAS_ALERTA_EDITAR");

    @Autowired
    private GestionDeReglas reglas;
    @Autowired
    private GestionDeDispositivos dispositivos;
    @Autowired
    private GestionDePreferencias preferencias;
    @Autowired
    private GestionDeDespacho despacho;
    @Autowired
    private ConsultaDeAlertas centro;
    @Autowired
    private PasarelaDePushStub push;
    @Autowired
    private PasarelaDeCorreoStub correo;

    private final UUID negocioA = UUID.randomUUID();
    private final UUID admin = UUID.randomUUID();
    private final UUID u1 = UUID.randomUUID();

    @BeforeEach
    void limpiar() {
        push.reiniciar();
        correo.reiniciar();
    }

    private void registrarDispositivo(String token) {
        enContexto(negocioA, u1, USUARIO, () -> dispositivos.registrar(
                new SolicitudDeDispositivo(token, "ANDROID", "Pixel", "1.0")));
    }

    private void reglaVencimiento(String nombre, String severidad, List<String> canales) {
        enContexto(negocioA, admin, ADMIN, () -> reglas.crear(new SolicitudDeRegla(
                "VENCIMIENTO_LOTE", nombre, null, Map.of(), severidad, canales,
                List.of(), List.of(u1), "INMEDIATA", null, 24)));
    }

    private List<UUID> evaluarLote(UUID lote) {
        return enContexto(negocioA, admin, ADMIN, () -> reglas.evaluar(new HechoDeAlerta(
                "VENCIMIENTO_LOTE", "Lote", lote, "/inventario/lotes/" + lote, null,
                Map.of("producto", "Amoxicilina", "lote", "L-9", "dias_para_vencer", 10,
                        "fecha_vencimiento", "2027-01-01"))));
    }

    @Test
    @DisplayName("Criterio 1: una alerta crítica llega por push al dispositivo Android activo")
    void pushAlDispositivo() {
        registrarDispositivo("token-abc");
        reglaVencimiento("Crítica push", "CRITICA", List.of("PUSH"));
        UUID lote = UUID.randomUUID();

        UUID alertaId = evaluarLote(lote).get(0);

        assertThat(push.tokensEnviados()).containsExactly("token-abc");
        assertThat(comoElServicio(negocioA,
                "select estado from entregas where alerta_id = '" + alertaId + "'"))
                .containsExactly("ENVIADA");
    }

    @Test
    @DisplayName("Criterio 2: la alerta in-app queda ENTREGADA y aparece en el centro del usuario")
    void inAppEnElCentro() {
        reglaVencimiento("In-app", "MEDIA", List.of("IN_APP"));
        UUID lote = UUID.randomUUID();

        UUID alertaId = evaluarLote(lote).get(0);

        assertThat(comoElServicio(negocioA,
                "select estado from entregas where alerta_id = '" + alertaId + "'"))
                .containsExactly("ENTREGADA");
        List<ConsultaDeAlertas.AlertaDelUsuario> mias = enContexto(negocioA, u1, USUARIO,
                () -> centro.mias());
        assertThat(mias).extracting(ConsultaDeAlertas.AlertaDelUsuario::id).containsExactly(alertaId);
    }

    @Test
    @DisplayName("Criterio 3: un envío que falla se reintenta con backoff y guarda el error")
    void reintentoConBackoff() {
        registrarDispositivo("token-fail");
        reglaVencimiento("Push falla", "ALTA", List.of("PUSH"));
        UUID lote = UUID.randomUUID();
        push.proximo(ResultadoDePush.fallo("FCM 503"));

        UUID alertaId = evaluarLote(lote).get(0);

        assertThat(comoElServicio(negocioA,
                "select estado from entregas where alerta_id = '" + alertaId + "'"))
                .containsExactly("FALLIDA");
        assertThat(comoElServicio(negocioA,
                "select error from entregas where alerta_id = '" + alertaId + "'"))
                .containsExactly("FCM 503");
        assertThat(comoElServicio(negocioA,
                "select (proximo_intento is not null) from entregas where alerta_id = '" + alertaId + "'"))
                .containsExactly("t");
        assertThat(comoElServicio(negocioA,
                "select intentos from entregas where alerta_id = '" + alertaId + "'"))
                .containsExactly("1");

        // Llega la hora del reintento y la pasarela ya responde bien.
        ejecutarComoElServicio(negocioA, "update entregas set proximo_intento = now() - interval '1 minute' "
                + "where alerta_id = '" + alertaId + "'");
        push.proximo(ResultadoDePush.ok("msg-2"));
        int tocadas = enContexto(negocioA, admin, ADMIN, () -> despacho.reintentarPendientes());

        assertThat(tocadas).isEqualTo(1);
        assertThat(comoElServicio(negocioA,
                "select estado from entregas where alerta_id = '" + alertaId + "'"))
                .containsExactly("ENVIADA");
    }

    @Test
    @DisplayName("Criterio 4: en la franja de no molestar la alerta no crítica se retiene; la crítica no")
    void noMolestarRetieneSalvoCritica() {
        LocalTime ahora = OffsetDateTime.now(ZoneOffset.UTC).toLocalTime();
        enContexto(negocioA, u1, USUARIO, () -> preferencias.fijar(new SolicitudDePreferencia(
                "VENCIMIENTO_LOTE", List.of("IN_APP"), true,
                ahora.minusHours(2), ahora.plusHours(2))));

        reglaVencimiento("No molestar media", "MEDIA", List.of("IN_APP"));
        UUID loteMedia = UUID.randomUUID();
        UUID alertaMedia = evaluarLote(loteMedia).get(0);
        assertThat(comoElServicio(negocioA,
                "select estado from entregas where alerta_id = '" + alertaMedia + "'"))
                .containsExactly("PENDIENTE");
        assertThat(comoElServicio(negocioA,
                "select (retenida_hasta is not null) from entregas where alerta_id = '" + alertaMedia + "'"))
                .containsExactly("t");

        reglaVencimiento("No molestar critica", "CRITICA", List.of("IN_APP"));
        UUID loteCritica = UUID.randomUUID();
        UUID alertaCritica = evaluarLote(loteCritica).get(0);
        assertThat(comoElServicio(negocioA,
                "select estado from entregas where alerta_id = '" + alertaCritica + "'"))
                .containsExactly("ENTREGADA");
    }

    @Test
    @DisplayName("Criterio 5: un token FCM inválido deja el dispositivo inactivo")
    void tokenInvalidoDesactivaDispositivo() {
        registrarDispositivo("token-muerto");
        reglaVencimiento("Push token muerto", "ALTA", List.of("PUSH"));
        UUID lote = UUID.randomUUID();
        push.proximo(ResultadoDePush.porTokenInvalido());

        evaluarLote(lote);

        assertThat(comoElServicio(negocioA,
                "select activo from dispositivos_push where token_fcm = 'token-muerto'"))
                .containsExactly("f");
    }

    @Test
    @DisplayName("Registrar el mismo token dos veces no duplica el dispositivo")
    void registrarTokenNoDuplica() {
        registrarDispositivo("token-unico");
        registrarDispositivo("token-unico");

        assertThat(comoElServicio(negocioA,
                "select count(*) from dispositivos_push where token_fcm = 'token-unico'"))
                .containsExactly("1");
    }

    @Test
    @DisplayName("Sin dispositivo push activo, la entrega push queda fallida con el motivo")
    void sinDispositivoPush() {
        reglaVencimiento("Push sin equipo", "ALTA", List.of("PUSH"));
        UUID lote = UUID.randomUUID();

        UUID alertaId = evaluarLote(lote).get(0);

        assertThat(comoElServicio(negocioA,
                "select estado from entregas where alerta_id = '" + alertaId + "'"))
                .containsExactly("FALLIDA");
        assertThat(comoElServicio(negocioA,
                "select error from entregas where alerta_id = '" + alertaId + "'").get(0))
                .contains("dispositivo push activo");
    }
}
