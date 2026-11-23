package com.regenta.alertas.infrastructure;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.regenta.alertas.application.PasarelaDePush;

/**
 * HU-123 · El push de verdad, por la API HTTP v1 de Firebase Cloud Messaging.
 *
 * <p>Se autentica con la cuenta de servicio del proyecto de Firebase, que llega SOLO por entorno
 * ({@code FCM_CREDENCIALES}: el JSON, tal cual o en base64). Con ella firma un JWT, lo cambia por un
 * token de acceso de Google (vale una hora y se reusa) y manda cada mensaje a
 * {@code /v1/projects/{proyecto}/messages:send}.
 *
 * <ul>
 *   <li>Salió: el identificador del mensaje de FCM queda en la entrega (C2).</li>
 *   <li>FCM dice que el token no sirve ({@code UNREGISTERED}, {@code SENDER_ID_MISMATCH}, o
 *       {@code INVALID_ARGUMENT} sobre el token): el dispositivo se desactiva (C3).</li>
 *   <li>Cuota, caída o red: falla y la entrega entra al reintento (HU-094).</li>
 * </ul>
 */
@Component
@ConditionalOnExpression("'${regenta.fcm.credenciales:}' != ''")
public class PasarelaFcm implements PasarelaDePush {

    private static final Logger LOG = LoggerFactory.getLogger(PasarelaFcm.class);
    static final String ALCANCE = "https://www.googleapis.com/auth/firebase.messaging";
    private static final Duration ESPERA = Duration.ofSeconds(10);

    private final ObjectMapper json;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(ESPERA).build();
    private final String proyecto;
    private final String correoDeServicio;
    private final RSAPrivateKey llave;
    private final String urlDelToken;
    private final String urlDeFcm;
    private volatile String tokenDeAcceso;
    private volatile Instant venceEn = Instant.EPOCH;

    public PasarelaFcm(ObjectMapper json, @Value("${regenta.fcm.credenciales:}") String credenciales,
            @Value("${regenta.fcm.url:https://fcm.googleapis.com}") String urlDeFcm) {
        this.json = json;
        try {
            JsonNode cuenta = json.readTree(texto(credenciales));
            this.proyecto = requerido(cuenta, "project_id");
            this.correoDeServicio = requerido(cuenta, "client_email");
            this.urlDelToken = cuenta.path("token_uri").asText("https://oauth2.googleapis.com/token");
            this.llave = llavePrivada(requerido(cuenta, "private_key"));
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception ilegible) {
            throw new IllegalStateException("FCM_CREDENCIALES no es la cuenta de servicio de Firebase (JSON)");
        }
        this.urlDeFcm = urlDeFcm.replaceAll("/+$", "");
        LOG.info("Push por Firebase Cloud Messaging, proyecto {}", proyecto);
    }

    @Override
    public ResultadoDePush enviar(String tokenFcm, String titulo, String cuerpo, Map<String, String> datos) {
        try {
            HttpResponse<String> r = mandar(tokenFcm, titulo, cuerpo, datos, tokenDeAcceso(false));
            if (r.statusCode() == 401) {
                // El token de acceso venció antes de tiempo o lo revocaron: uno nuevo y otra vez.
                r = mandar(tokenFcm, titulo, cuerpo, datos, tokenDeAcceso(true));
            }
            if (r.statusCode() == 200) {
                String nombre = json.readTree(r.body()).path("name").asText("");
                return ResultadoDePush.ok(nombre.contains("/messages/")
                        ? nombre.substring(nombre.lastIndexOf("/messages/") + 10) : nombre);
            }
            return interpretarError(r.statusCode(), r.body());
        } catch (IOException | IllegalStateException fallo) {
            return ResultadoDePush.fallo("FCM no respondió: " + fallo.getMessage());
        } catch (InterruptedException cortado) {
            Thread.currentThread().interrupt();
            return ResultadoDePush.fallo("Se interrumpió el envío a FCM");
        }
    }

    private HttpResponse<String> mandar(String tokenFcm, String titulo, String cuerpo, Map<String, String> datos,
            String acceso) throws IOException, InterruptedException {
        Map<String, Object> mensaje = new LinkedHashMap<>();
        mensaje.put("token", tokenFcm);
        mensaje.put("notification", Map.of("title", titulo == null ? "" : titulo, "body", cuerpo == null ? "" : cuerpo));
        if (datos != null && !datos.isEmpty()) {
            mensaje.put("data", datos);
        }
        mensaje.put("android", Map.of("priority", "high"));
        String cuerpoJson = json.writeValueAsString(Map.of("message", mensaje));
        return http.send(HttpRequest.newBuilder(URI.create(urlDeFcm + "/v1/projects/" + proyecto + "/messages:send"))
                .timeout(ESPERA)
                .header("Authorization", "Bearer " + acceso)
                .header("Content-Type", "application/json; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(cuerpoJson, StandardCharsets.UTF_8))
                .build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    /** Qué hacer con el error de FCM: el token no sirve (se desactiva el dispositivo) o se reintenta. */
    ResultadoDePush interpretarError(int status, String cuerpo) {
        String estado = "";
        String codigo = "";
        String mensaje = "";
        try {
            JsonNode error = json.readTree(cuerpo == null ? "{}" : cuerpo).path("error");
            estado = error.path("status").asText("");
            mensaje = error.path("message").asText("");
            for (JsonNode detalle : error.path("details")) {
                if (detalle.has("errorCode")) {
                    codigo = detalle.path("errorCode").asText("");
                }
            }
        } catch (IOException noEsJson) {
            // queda el status HTTP
        }
        boolean tokenMalo = "UNREGISTERED".equals(codigo) || "SENDER_ID_MISMATCH".equals(codigo)
                || status == 404
                || (("INVALID_ARGUMENT".equals(codigo) || "INVALID_ARGUMENT".equals(estado))
                        && mensaje.toLowerCase().contains("token"));
        if (tokenMalo) {
            return new ResultadoDePush(null, false, true,
                    "FCM: el token no es válido (" + (codigo.isEmpty() ? estado : codigo) + ")");
        }
        return ResultadoDePush.fallo("FCM respondió " + status + (codigo.isEmpty() ? "" : " " + codigo)
                + (mensaje.isEmpty() ? "" : ": " + mensaje));
    }

    /** El token de acceso de Google, reusado hasta cinco minutos antes de vencer. */
    private synchronized String tokenDeAcceso(boolean renovar) throws IOException, InterruptedException {
        if (!renovar && tokenDeAcceso != null && Instant.now().isBefore(venceEn.minusSeconds(300))) {
            return tokenDeAcceso;
        }
        Instant ahora = Instant.now();
        String asercion;
        try {
            SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), new JWTClaimsSet.Builder()
                    .issuer(correoDeServicio).audience(urlDelToken).claim("scope", ALCANCE)
                    .issueTime(Date.from(ahora)).expirationTime(Date.from(ahora.plusSeconds(3600))).build());
            jwt.sign(new RSASSASigner(llave));
            asercion = jwt.serialize();
        } catch (Exception imposible) {
            throw new IllegalStateException("No se pudo firmar la aserción para Google", imposible);
        }
        String forma = "grant_type=" + URLEncoder.encode("urn:ietf:params:oauth:grant-type:jwt-bearer", StandardCharsets.UTF_8)
                + "&assertion=" + URLEncoder.encode(asercion, StandardCharsets.UTF_8);
        HttpResponse<String> r = http.send(HttpRequest.newBuilder(URI.create(urlDelToken)).timeout(ESPERA)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(forma)).build(), HttpResponse.BodyHandlers.ofString());
        if (r.statusCode() != 200) {
            throw new IllegalStateException("Google no dio el token de acceso (HTTP " + r.statusCode() + ")");
        }
        JsonNode respuesta = json.readTree(r.body());
        tokenDeAcceso = respuesta.path("access_token").asText();
        venceEn = ahora.plusSeconds(respuesta.path("expires_in").asLong(3600));
        return tokenDeAcceso;
    }

    private static String texto(String credenciales) {
        String c = credenciales.trim();
        return c.startsWith("{") ? c : new String(Base64.getMimeDecoder().decode(c), StandardCharsets.UTF_8);
    }

    private static String requerido(JsonNode cuenta, String campo) {
        String v = cuenta.path(campo).asText("");
        if (v.isBlank()) {
            throw new IllegalStateException("FCM_CREDENCIALES no trae " + campo);
        }
        return v;
    }

    private static RSAPrivateKey llavePrivada(String pem) throws Exception {
        String base64 = pem.replaceAll("-----[A-Z ]+-----", "").replaceAll("\\s", "");
        return (RSAPrivateKey) KeyFactory.getInstance("RSA")
                .generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(base64)));
    }
}
