package com.regenta.facturacion.application;

import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Collections;

import com.regenta.shared.web.ReglaDeNegocioException;

/**
 * HU-116 · Un certificado de firma ya abierto: la llave privada y su certificado X.509. Se arma desde el
 * .p12 que sube el negocio (con su clave) y nunca se guarda así: a la bóveda va el sobre de
 * {@link #sobre}, cifrado.
 */
public record CertificadoDeFirma(PrivateKey llave, X509Certificate certificado) {

    /** Abre el .p12; con una clave equivocada o sin llave privada, 422. */
    public static CertificadoDeFirma abrir(byte[] p12, String clave) {
        if (p12 == null || p12.length == 0) {
            throw new ReglaDeNegocioException("Falta el archivo .p12 del certificado");
        }
        try {
            KeyStore almacen = KeyStore.getInstance("PKCS12");
            char[] secreto = clave == null ? new char[0] : clave.toCharArray();
            almacen.load(new ByteArrayInputStream(p12), secreto);
            for (String alias : Collections.list(almacen.aliases())) {
                if (almacen.isKeyEntry(alias)
                        && almacen.getKey(alias, secreto) instanceof PrivateKey llave
                        && almacen.getCertificate(alias) instanceof X509Certificate x509) {
                    return new CertificadoDeFirma(llave, x509);
                }
            }
        } catch (Exception ilegible) {
            throw new ReglaDeNegocioException(
                    "No se pudo abrir el certificado: revisa el archivo .p12 y su clave");
        }
        throw new ReglaDeNegocioException("El archivo .p12 no trae una llave privada para firmar");
    }

    /** Lo que va (cifrado) a la bóveda: el .p12 y su clave, juntos. */
    public static byte[] sobre(byte[] p12, String clave) {
        byte[] c = (clave == null ? "" : clave).getBytes(StandardCharsets.UTF_8);
        return ByteBuffer.allocate(4 + c.length + p12.length).putInt(c.length).put(c).put(p12).array();
    }

    /** Lo que sale de la bóveda, ya descifrado, vuelve a abrirse. */
    public static CertificadoDeFirma desdeSobre(byte[] sobre) {
        ByteBuffer b = ByteBuffer.wrap(sobre);
        byte[] c = new byte[b.getInt()];
        b.get(c);
        byte[] p12 = new byte[b.remaining()];
        b.get(p12);
        return abrir(p12, new String(c, StandardCharsets.UTF_8));
    }

    public LocalDate vigenteDesde() {
        return certificado.getNotBefore().toInstant().atZone(ZoneOffset.UTC).toLocalDate();
    }

    public LocalDate vigenteHasta() {
        return certificado.getNotAfter().toInstant().atZone(ZoneOffset.UTC).toLocalDate();
    }

    public String emisor() {
        String dn = certificado.getIssuerX500Principal().getName();
        return dn.length() > 150 ? dn.substring(0, 150) : dn;
    }

    public String numeroSerie() {
        String serie = certificado.getSerialNumber().toString(16).toUpperCase();
        return serie.length() > 80 ? serie.substring(0, 80) : serie;
    }
}
