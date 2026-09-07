package com.regenta.facturacion.aplicacion;

import com.regenta.facturacion.domain.Factura;

/**
 * Firma el XML de la factura y calcula su CUFE. La implementación real usa el
 * certificado (bytes que da {@link BovedaDeSecretos}); en pruebas se sustituye.
 */
public interface FirmadorDeXml {

    FirmaDeFactura firmar(Factura factura, byte[] certificado);

    /** El resultado de firmar: el CUFE y el XML ya firmado. */
    record FirmaDeFactura(String cufe, byte[] xmlFirmado) {
    }
}
