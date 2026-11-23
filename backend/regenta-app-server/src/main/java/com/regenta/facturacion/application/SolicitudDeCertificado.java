package com.regenta.facturacion.application;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * HU-116 · Alta de un certificado de firma: el archivo .p12 (en base64) y su clave. Emisor, serie y
 * vigencia se leen del propio certificado. El archivo y la clave van cifrados a la bóveda; a la base
 * solo llega la referencia.
 */
public record SolicitudDeCertificado(
        @NotBlank @Size(max = 80) String alias,
        @NotBlank String p12Base64,
        String clave) {
}
