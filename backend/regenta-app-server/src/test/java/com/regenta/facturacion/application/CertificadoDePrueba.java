package com.regenta.facturacion.application;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Date;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

/**
 * HU-116 · Un certificado de firma de prueba, generado al vuelo: RSA 2048 autofirmado, empaquetado en
 * un .p12 con clave. Nunca hay un certificado real en el repo.
 */
public final class CertificadoDePrueba {

    private CertificadoDePrueba() {
    }

    /** Un .p12 con la clave dada, vigente entre esas fechas. */
    public static byte[] p12(String clave, LocalDate desde, LocalDate hasta) {
        try {
            KeyPairGenerator generador = KeyPairGenerator.getInstance("RSA");
            generador.initialize(2048);
            KeyPair par = generador.generateKeyPair();
            X500Name nombre = new X500Name("CN=Negocio de Prueba, O=Regenta, C=CO");
            X509Certificate cert = new JcaX509CertificateConverter().getCertificate(
                    new JcaX509v3CertificateBuilder(nombre, BigInteger.valueOf(System.nanoTime()),
                            Date.from(desde.atStartOfDay().toInstant(ZoneOffset.UTC)),
                            Date.from(hasta.atTime(23, 0).toInstant(ZoneOffset.UTC)), nombre, par.getPublic())
                            .build(new JcaContentSignerBuilder("SHA256withRSA").build(par.getPrivate())));
            KeyStore ks = KeyStore.getInstance("PKCS12");
            ks.load(null, null);
            ks.setKeyEntry("firma", par.getPrivate(), clave.toCharArray(), new Certificate[] {cert});
            ByteArrayOutputStream salida = new ByteArrayOutputStream();
            ks.store(salida, clave.toCharArray());
            return salida.toByteArray();
        } catch (Exception fallo) {
            throw new IllegalStateException(fallo);
        }
    }

    /** Vigente un año desde ayer. */
    public static byte[] vigente(String clave) {
        return p12(clave, LocalDate.now().minusDays(1), LocalDate.now().plusYears(1));
    }
}
