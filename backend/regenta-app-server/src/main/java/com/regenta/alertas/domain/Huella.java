package com.regenta.alertas.domain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

/**
 * La huella de deduplicación de una alerta (HU-092 criterio 3). Es determinista
 * a partir de la regla y la entidad afectada: la misma regla sobre el mismo
 * lote/producto produce siempre la misma huella, y {@code uq_alerta_huella}
 * impide la fila repetida.
 */
public final class Huella {

    private Huella() {
    }

    public static String de(ReglaAlerta regla, String entidadTipo, UUID entidadId) {
        String base = regla.getId() + "|" + regla.getTipoCodigo() + "|"
                + (entidadTipo == null ? "" : entidadTipo) + "|"
                + (entidadId == null ? "" : entidadId);
        return sha256Hex(base);
    }

    private static String sha256Hex(String texto) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(texto.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(Character.forDigit((b >> 4) & 0xF, 16));
                hex.append(Character.forDigit(b & 0xF, 16));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException imposible) {
            throw new IllegalStateException(imposible);
        }
    }
}
