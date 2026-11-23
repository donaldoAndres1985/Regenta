package com.regenta.facturacion.application;

/**
 * Arma el XML UBL de la factura, calcula su CUFE oficial y lo firma con el certificado del negocio, ya
 * abierto desde la bóveda (HU-116, HU-117).
 */
public interface FirmadorDeXml {

    FirmaDeFactura firmar(DocumentoAFirmar documento, CertificadoDeFirma certificado);

    /** El resultado de firmar: el CUFE y el XML ya firmado. */
    record FirmaDeFactura(String cufe, byte[] xmlFirmado) {
    }
}
