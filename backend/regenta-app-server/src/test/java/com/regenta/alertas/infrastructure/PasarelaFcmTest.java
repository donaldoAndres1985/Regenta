package com.regenta.alertas.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.regenta.alertas.application.PasarelaDePush.ResultadoDePush;

/** HU-123 · La pasarela de FCM v1 contra el FCM simulado por contrato. */
class PasarelaFcmTest {

    private FcmSimulado fcm;
    private PasarelaFcm pasarela;

    @BeforeEach
    void arrancar() throws Exception {
        fcm = new FcmSimulado();
        pasarela = new PasarelaFcm(new ObjectMapper(), fcm.credenciales(), fcm.url());
    }

    @AfterEach
    void parar() {
        fcm.close();
    }

    @Test
    void c2_salioYDevuelveElIdentificadorDeFcm() {
        ResultadoDePush r = pasarela.enviar("token-1", "Stock bajo: cemento", "Quedan 3 bultos",
                Map.of("alerta_id", "a-1", "ruta", "/inventario"));

        assertThat(r.exito()).isTrue();
        assertThat(r.proveedorId()).startsWith("0:17000");
        assertThat(fcm.envios).singleElement().satisfies(e -> {
            assertThat(e.token()).isEqualTo("token-1");
            assertThat(e.titulo()).isEqualTo("Stock bajo: cemento");
            assertThat(e.datos()).containsEntry("ruta", "/inventario");
        });
    }

    @Test
    void c3_unTokenNoRegistradoSeReportaInvalido() {
        fcm.responder("viejo", 404, FcmSimulado.noRegistrado());

        ResultadoDePush r = pasarela.enviar("viejo", "t", "c", Map.of());

        assertThat(r.exito()).isFalse();
        assertThat(r.tokenInvalido()).isTrue();
        assertThat(r.error()).contains("UNREGISTERED");
    }

    @Test
    void c3_tokenDeOtroProyectoOMalFormadoTambienEsInvalido() {
        assertThat(pasarela.interpretarError(403, "{\"error\":{\"status\":\"PERMISSION_DENIED\",\"details\":"
                + "[{\"errorCode\":\"SENDER_ID_MISMATCH\"}]}}").tokenInvalido()).isTrue();
        assertThat(pasarela.interpretarError(400, "{\"error\":{\"status\":\"INVALID_ARGUMENT\",\"message\":"
                + "\"The registration token is not a valid FCM registration token\"}}").tokenInvalido()).isTrue();
        // Otro INVALID_ARGUMENT (el mensaje, no el token) no apaga el dispositivo.
        assertThat(pasarela.interpretarError(400, "{\"error\":{\"status\":\"INVALID_ARGUMENT\",\"message\":"
                + "\"Invalid JSON payload\"}}").tokenInvalido()).isFalse();
    }

    @Test
    void unaCaidaDeFcmNoApagaElDispositivoYSeReintenta() {
        fcm.responder("token-1", 503, FcmSimulado.noDisponible());

        ResultadoDePush r = pasarela.enviar("token-1", "t", "c", Map.of());

        assertThat(r.exito()).isFalse();
        assertThat(r.tokenInvalido()).isFalse();
        assertThat(r.error()).contains("503");
    }

    @Test
    void elTokenDeAccesoSeReusaYSeRenuevaSiFcmLoRechaza() {
        pasarela.enviar("a", "t", "c", Map.of());
        pasarela.enviar("b", "t", "c", Map.of());
        assertThat(fcm.tokensDeAcceso.get()).isEqualTo(1);

        fcm.rechazarConAccesoVencido = 1;
        ResultadoDePush r = pasarela.enviar("c", "t", "c", Map.of());

        assertThat(r.exito()).isTrue();
        assertThat(fcm.tokensDeAcceso.get()).isEqualTo(2);
    }

    @Test
    void lasCredencialesPuedenVenirEnBase64() {
        String base64 = Base64.getEncoder().encodeToString(fcm.credenciales().getBytes(StandardCharsets.UTF_8));
        PasarelaFcm otra = new PasarelaFcm(new ObjectMapper(), base64, fcm.url());

        assertThat(otra.enviar("x", "t", "c", Map.of()).exito()).isTrue();
    }

    @Test
    void conCredencialesQueNoSonDeFirebaseNoArranca() {
        assertThatThrownBy(() -> new PasarelaFcm(new ObjectMapper(), "{\"project_id\":\"p\"}", fcm.url()))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("client_email");
    }
}
