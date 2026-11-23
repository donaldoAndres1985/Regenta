package com.regenta.facturacion.application;

import com.regenta.facturacion.domain.Factura;

/**
 * Firma el XML de la factura y calcula su CUFE con el certificado del negocio, ya abierto desde la
 * bóveda (HU-116).
 */
public interface FirmadorDeXml {

    FirmaDeFactura firmar(Factura factura, CertificadoDeFirma certificado);

    /** El resultado de firmar: el CUFE y el XML ya firmado. */
    record FirmaDeFactura(String cufe, byte[] xmlFirmado) {
    }
}
