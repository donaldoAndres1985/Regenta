package com.regenta.facturacion.infrastructure;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.regenta.facturacion.application.BovedaDeSecretos;
import com.regenta.shared.almacen.AlmacenDeObjetos;
import com.regenta.shared.web.NoEncontradoException;

/**
 * HU-116 · La bóveda del certificado de firma. El secreto (el .p12 y su clave) se cifra con
 * AES-256-GCM y se guarda en el almacén de objetos (HU-121), nunca en la base del servicio (C1).
 *
 * <p>Cada negocio cifra con su propia llave, derivada de la llave maestra con HMAC-SHA256 y el id del
 * negocio, y el id va además como dato autenticado. El objeto vive bajo la carpeta del negocio. Así,
 * otro negocio no lo encuentra, y aunque alguien copiara el cifrado, con la llave de otro negocio no
 * se descifra (C5).
 *
 * <p>La llave maestra llega SOLO por entorno ({@code BOVEDA_LLAVE_MAESTRA}, 32 bytes en base64). Sin
 * ella se usa una llave al azar de este arranque, para desarrollo: lo guardado deja de poder leerse al
 * reiniciar, y por eso se avisa en el log.
 */
@Component
public class BovedaCifrada implements BovedaDeSecretos {

    private static final Logger LOG = LoggerFactory.getLogger(BovedaCifrada.class);
    private static final byte VERSION = 1;
    private static final int IV = 12;
    private static final int ETIQUETA = 128;
    static final String CARPETA = "boveda";

    private final AlmacenDeObjetos almacen;
    private final byte[] maestra;
    private final SecureRandom azar = new SecureRandom();

    public BovedaCifrada(AlmacenDeObjetos almacen, @Value("${regenta.boveda.llave-maestra:}") String llaveMaestra) {
        this.almacen = almacen;
        this.maestra = maestra(llaveMaestra);
    }

    private byte[] maestra(String base64) {
        if (base64 == null || base64.isBlank()) {
            LOG.warn("Sin BOVEDA_LLAVE_MAESTRA: la bóveda usa una llave de este arranque y lo guardado no "
                    + "sobrevive a un reinicio. Configúrala en producción.");
            byte[] llave = new byte[32];
            azar.nextBytes(llave);
            return llave;
        }
        byte[] llave = Base64.getDecoder().decode(base64.trim());
        if (llave.length != 32) {
            throw new IllegalStateException("BOVEDA_LLAVE_MAESTRA debe ser de 32 bytes en base64 (AES-256)");
        }
        return llave;
    }

    @Override
    public String guardar(UUID negocioId, String nombre, byte[] secreto) {
        return almacen.guardar(negocioId, CARPETA, nombre + ".cifrado", cifrar(negocioId, secreto),
                "application/octet-stream");
    }

    @Override
    public byte[] leer(UUID negocioId, String referencia) {
        return descifrar(negocioId, almacen.leer(negocioId, referencia));
    }

    /** {@code versión · iv · cifrado+etiqueta}, con el negocio como dato autenticado. */
    byte[] cifrar(UUID negocioId, byte[] secreto) {
        try {
            byte[] iv = new byte[IV];
            azar.nextBytes(iv);
            Cipher aes = Cipher.getInstance("AES/GCM/NoPadding");
            aes.init(Cipher.ENCRYPT_MODE, llaveDe(negocioId), new GCMParameterSpec(ETIQUETA, iv));
            aes.updateAAD(negocioId.toString().getBytes(StandardCharsets.UTF_8));
            byte[] cifrado = aes.doFinal(secreto);
            return ByteBuffer.allocate(1 + IV + cifrado.length).put(VERSION).put(iv).put(cifrado).array();
        } catch (GeneralSecurityException fallo) {
            throw new IllegalStateException("No se pudo cifrar el secreto", fallo);
        }
    }

    byte[] descifrar(UUID negocioId, byte[] sobre) {
        if (sobre == null || sobre.length < 1 + IV + 16 || sobre[0] != VERSION) {
            throw new NoEncontradoException("Ese certificado no existe");
        }
        try {
            Cipher aes = Cipher.getInstance("AES/GCM/NoPadding");
            aes.init(Cipher.DECRYPT_MODE, llaveDe(negocioId), new GCMParameterSpec(ETIQUETA, sobre, 1, IV));
            aes.updateAAD(negocioId.toString().getBytes(StandardCharsets.UTF_8));
            return aes.doFinal(sobre, 1 + IV, sobre.length - 1 - IV);
        } catch (GeneralSecurityException noEsDeEsteNegocio) {
            // Otra llave (otro negocio, otra maestra) o el sobre alterado: para quien pregunta no existe.
            throw new NoEncontradoException("Ese certificado no existe");
        }
    }

    private SecretKeySpec llaveDe(UUID negocioId) throws GeneralSecurityException {
        Mac hmac = Mac.getInstance("HmacSHA256");
        hmac.init(new SecretKeySpec(maestra, "HmacSHA256"));
        byte[] llave = hmac.doFinal(("regenta-boveda:" + negocioId).getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(llave, "AES");
    }
}
