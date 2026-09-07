package com.regenta.facturacion.infra;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

import org.springframework.stereotype.Component;

import com.regenta.facturacion.aplicacion.FirmadorDeXml;
import com.regenta.facturacion.domain.Factura;

/**
 * Stub de la firma. Calcula un CUFE determinista (SHA-256 de los datos clave) y
 * arma un XML mínimo. La integración real (UBL 2.1 + XAdES-EPES con el
 * certificado) reemplaza este bean.
 */
@Component
public class FirmadorDeXmlStub implements FirmadorDeXml {

    @Override
    public FirmaDeFactura firmar(Factura factura, byte[] certificado) {
        String semilla = factura.getNumeroCompleto() + "|" + factura.getTotal() + "|"
                + factura.getFechaEmision() + "|" + certificado.length;
        String cufe;
        try {
            // SHA-384 -> 96 hex, el mismo largo que el CUFE real.
            byte[] hash = MessageDigest.getInstance("SHA-384")
                    .digest(semilla.getBytes(StandardCharsets.UTF_8));
            cufe = HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo calcular el CUFE", e);
        }
        String xml = "<Invoice><CUFE>" + cufe + "</CUFE><Numero>"
                + factura.getNumeroCompleto() + "</Numero></Invoice>";
        return new FirmaDeFactura(cufe, xml.getBytes(StandardCharsets.UTF_8));
    }
}
