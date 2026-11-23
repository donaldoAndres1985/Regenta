package com.regenta.alertas.infrastructure;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

/**
 * HU-123 · FCM simulado por contrato: un servidor local que hace de Google OAuth ({@code /token}) y de
 * la API HTTP v1 de FCM ({@code /v1/projects/{p}/messages:send}). Revisa lo que haría Google: la aserción
 * firmada con la llave de la cuenta de servicio y el alcance de mensajería, y el token de acceso en cada
 * envío. Responde según el token del dispositivo.
 */
public final class FcmSimulado implements AutoCloseable {

    public static final String PROYECTO = "regenta-prueba";

    /** Lo que recibió en cada envío. */
    public record Envio(String token, String titulo, String cuerpo, Map<String, String> datos) {
    }

    private final HttpServer servidor;
    private final ObjectMapper json = new ObjectMapper();
    private final KeyPair llaves;
    private final Map<String, String[]> respuestas = new ConcurrentHashMap<>();
    public final List<Envio> envios = new ArrayList<>();
    public final AtomicInteger tokensDeAcceso = new AtomicInteger();
    public volatile int rechazarConAccesoVencido = 0;
    private final AtomicInteger mensajes = new AtomicInteger();

    public FcmSimulado() throws Exception {
        KeyPairGenerator g = KeyPairGenerator.getInstance("RSA");
        g.initialize(2048);
        llaves = g.generateKeyPair();
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/token", this::token);
        servidor.createContext("/v1/projects/", this::enviar);
        servidor.start();
    }

    public String url() {
        return "http://127.0.0.1:" + servidor.getAddress().getPort();
    }

    /** La cuenta de servicio, como la descarga la consola de Firebase. */
    public String credenciales() {
        String pem = "-----BEGIN PRIVATE KEY-----\n"
                + Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(llaves.getPrivate().getEncoded())
                + "\n-----END PRIVATE KEY-----\n";
        try {
            return json.writeValueAsString(Map.of("type", "service_account", "project_id", PROYECTO,
                    "private_key_id", "abc", "private_key", pem,
                    "client_email", "push@" + PROYECTO + ".iam.gserviceaccount.com", "token_uri", url() + "/token"));
        } catch (Exception imposible) {
            throw new IllegalStateException(imposible);
        }
    }

    /** Lo que FCM contesta para ese token de dispositivo: {status, cuerpo}. Sin fijar, lo acepta. */
    public void responder(String tokenDispositivo, int status, String cuerpo) {
        respuestas.put(tokenDispositivo, new String[] {Integer.toString(status), cuerpo});
    }

    public static String noRegistrado() {
        return "{\"error\":{\"code\":404,\"message\":\"Requested entity was not found.\",\"status\":\"NOT_FOUND\","
                + "\"details\":[{\"@type\":\"type.googleapis.com/google.firebase.fcm.v1.FcmError\","
                + "\"errorCode\":\"UNREGISTERED\"}]}}";
    }

    public static String noDisponible() {
        return "{\"error\":{\"code\":503,\"message\":\"The service is currently unavailable.\",\"status\":\"UNAVAILABLE\","
                + "\"details\":[{\"@type\":\"type.googleapis.com/google.firebase.fcm.v1.FcmError\","
                + "\"errorCode\":\"UNAVAILABLE\"}]}}";
    }

    private void token(HttpExchange e) throws IOException {
        String forma = new String(e.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        String asercion = null;
        for (String par : forma.split("&")) {
            if (par.startsWith("assertion=")) {
                asercion = URLDecoder.decode(par.substring(10), StandardCharsets.UTF_8);
            }
        }
        try {
            SignedJWT jwt = SignedJWT.parse(asercion);
            boolean valida = jwt.verify(new RSASSAVerifier((RSAPublicKey) llaves.getPublic()))
                    && PasarelaFcm.ALCANCE.equals(jwt.getJWTClaimsSet().getStringClaim("scope"))
                    && jwt.getJWTClaimsSet().getAudience().contains(url() + "/token");
            if (!valida) {
                responder(e, 400, "{\"error\":\"invalid_grant\"}");
                return;
            }
        } catch (Exception mala) {
            responder(e, 400, "{\"error\":\"invalid_grant\"}");
            return;
        }
        int n = tokensDeAcceso.incrementAndGet();
        responder(e, 200, "{\"access_token\":\"acceso-" + n + "\",\"expires_in\":3599,\"token_type\":\"Bearer\"}");
    }

    private void enviar(HttpExchange e) throws IOException {
        String autorizacion = e.getRequestHeaders().getFirst("Authorization");
        if (autorizacion == null || !autorizacion.startsWith("Bearer acceso-")) {
            responder(e, 401, "{\"error\":{\"code\":401,\"status\":\"UNAUTHENTICATED\"}}");
            return;
        }
        if (rechazarConAccesoVencido > 0) {
            rechazarConAccesoVencido--;
            responder(e, 401, "{\"error\":{\"code\":401,\"status\":\"UNAUTHENTICATED\"}}");
            return;
        }
        if (!e.getRequestURI().getPath().equals("/v1/projects/" + PROYECTO + "/messages:send")) {
            responder(e, 404, "{}");
            return;
        }
        JsonNode mensaje = json.readTree(e.getRequestBody().readAllBytes()).path("message");
        String token = mensaje.path("token").asText();
        @SuppressWarnings("unchecked")
        Map<String, String> datos = json.convertValue(mensaje.path("data"), Map.class);
        synchronized (envios) {
            envios.add(new Envio(token, mensaje.path("notification").path("title").asText(),
                    mensaje.path("notification").path("body").asText(), datos));
        }
        String[] fija = respuestas.get(token);
        if (fija != null) {
            responder(e, Integer.parseInt(fija[0]), fija[1]);
            return;
        }
        responder(e, 200, "{\"name\":\"projects/" + PROYECTO + "/messages/0:17000" + mensajes.incrementAndGet() + "\"}");
    }

    private static void responder(HttpExchange e, int status, String cuerpo) throws IOException {
        byte[] b = cuerpo.getBytes(StandardCharsets.UTF_8);
        e.getResponseHeaders().add("Content-Type", "application/json");
        e.sendResponseHeaders(status, b.length);
        try (OutputStream s = e.getResponseBody()) {
            s.write(b);
        }
    }

    @Override
    public void close() {
        servidor.stop(0);
    }
}
