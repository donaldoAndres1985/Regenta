package com.regenta.usuarios.infra;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.Collection;
import java.util.Date;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

/**
 * Emite el token que valida el gateway. HU-013.
 *
 * <p>Los claims son el contrato con el gateway y con los quince servicios:
 * negocio, plan, patron, roles, modulos, permisos, sucursales y el estado del
 * negocio. Estan documentados en backend/README.md y en el final de la
 * migracion V1; cambiarlos aqui sin cambiarlos alla rompe el borde.
 *
 * <p>El refresh token no es un JWT: es un secreto opaco con el negocio delante.
 * Lleva el negocio porque la tabla que lo guarda tiene RLS, y sin saber a que
 * negocio pertenece no hay forma de buscarlo. De el solo se guarda el SHA-256.
 */
@Component
public class EmisorDeTokens {

    private static final SecureRandom AZAR = new SecureRandom();

    private final PropiedadesDeToken propiedades;
    private final Clock reloj;

    public EmisorDeTokens(PropiedadesDeToken propiedades, Clock reloj) {
        this.propiedades = propiedades;
        this.reloj = reloj;
        if (propiedades.getSecreto() == null || propiedades.getSecreto().trim().length() < 32) {
            throw new IllegalStateException("Falta el secreto del JWT: definir JWT_SECRETO"
                    + " (regenta.jwt.secreto) con al menos 32 caracteres. Es el mismo con el"
                    + " que valida el gateway");
        }
    }

    /** Datos que el token lleva encima. */
    public record Contexto(UUID negocio, UUID usuario, String plan, String patron,
            String estadoNegocio, Collection<String> roles, Collection<String> modulos,
            Collection<String> permisos, Collection<UUID> sucursales) {
    }

    public record TokenDeAcceso(String valor, long vigenciaEnSegundos, Instant expiraEn) {
    }

    public TokenDeAcceso emitir(Contexto contexto) {
        Instant ahora = Instant.now(reloj);
        Instant expira = ahora.plus(propiedades.getVigenciaAcceso());
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(propiedades.getEmisor())
                .subject(contexto.usuario().toString())
                .jwtID(UUID.randomUUID().toString())
                .issueTime(Date.from(ahora))
                .expirationTime(Date.from(expira))
                .claim("negocio_id", contexto.negocio().toString())
                .claim("plan", contexto.plan())
                .claim("patron", contexto.patron())
                .claim("estado_negocio", contexto.estadoNegocio())
                .claim("roles", List.copyOf(contexto.roles()))
                .claim("modulos", List.copyOf(contexto.modulos()))
                .claim("permisos", List.copyOf(contexto.permisos()))
                .claim("sucursales", contexto.sucursales().stream().map(UUID::toString).toList())
                .build();
        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        try {
            jwt.sign(new MACSigner(propiedades.getSecreto().getBytes(StandardCharsets.UTF_8)));
        } catch (JOSEException fallo) {
            throw new IllegalStateException("No se pudo firmar el token", fallo);
        }
        return new TokenDeAcceso(jwt.serialize(),
                propiedades.getVigenciaAcceso().toSeconds(), expira);
    }

    /** {@code <negocio>.<secreto>}: el negocio delante para poder buscarlo con RLS. */
    public String nuevoRefresco(UUID negocio) {
        byte[] secreto = new byte[32];
        AZAR.nextBytes(secreto);
        return negocio + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(secreto);
    }

    public static UUID negocioDe(String tokenDeRefresco) {
        if (tokenDeRefresco == null) {
            return null;
        }
        int punto = tokenDeRefresco.indexOf('.');
        if (punto <= 0) {
            return null;
        }
        try {
            return UUID.fromString(tokenDeRefresco.substring(0, punto));
        } catch (IllegalArgumentException noEsUuid) {
            return null;
        }
    }

    public static String hash(String token) {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(sha.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException imposible) {
            throw new IllegalStateException("Esta JVM no trae SHA-256", imposible);
        }
    }

    public Instant expiracionDelRefresco() {
        return Instant.now(reloj).plus(propiedades.getVigenciaRefresco());
    }
}
