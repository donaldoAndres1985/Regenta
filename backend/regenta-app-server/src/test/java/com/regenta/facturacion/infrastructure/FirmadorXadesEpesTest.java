package com.regenta.facturacion.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;

import com.regenta.facturacion.application.CertificadoDeFirma;
import com.regenta.facturacion.application.CertificadoDePrueba;
import com.regenta.facturacion.application.FirmadorDeXml.FirmaDeFactura;
import com.regenta.facturacion.domain.Factura;

/** HU-116 C2 · La firma XAdES-EPES valida contra el certificado, y solo contra ese. */
class FirmadorXadesEpesTest {

    private final FirmadorXadesEpes firmador = new FirmadorXadesEpes();
    private final CertificadoDeFirma certificado =
            CertificadoDeFirma.abrir(CertificadoDePrueba.vigente("clave-1"), "clave-1");

    @Test
    void c2_laFirmaValidaContraElCertificado() {
        FirmaDeFactura firma = firmador.firmar(factura(), certificado);

        assertThat(firma.cufe()).hasSize(96);
        assertThat(FirmadorXadesEpes.verificar(firma.xmlFirmado(), certificado.certificado())).isTrue();
    }

    @Test
    void c2_esXadesEpesDentroDeLaExtensionUbl() {
        String xml = new String(firmador.firmar(factura(), certificado).xmlFirmado(), StandardCharsets.UTF_8);

        assertThat(xml)
                .contains("ext:ExtensionContent><ds:Signature")
                .contains("xades:QualifyingProperties")
                .contains("xades:SigningTime")
                .contains("xades:SigningCertificate")
                .contains("xades:SignaturePolicyIdentifier")
                .contains(FirmadorXadesEpes.POLITICA)
                .contains("http://uri.etsi.org/01903#SignedProperties")
                .contains("rsa-sha256")
                .contains("<cbc:ID>SETT-15</cbc:ID>");
    }

    @Test
    void c2_unXmlAlteradoYaNoValida() {
        byte[] xml = firmador.firmar(factura(), certificado).xmlFirmado();
        String alterado = new String(xml, StandardCharsets.UTF_8).replace("119000.00", "1.00");

        assertThat(FirmadorXadesEpes.verificar(alterado.getBytes(StandardCharsets.UTF_8),
                certificado.certificado())).isFalse();
    }

    @Test
    void c2_conOtroCertificadoNoValida() {
        byte[] xml = firmador.firmar(factura(), certificado).xmlFirmado();
        CertificadoDeFirma otro = CertificadoDeFirma.abrir(CertificadoDePrueba.vigente("otra"), "otra");

        assertThat(FirmadorXadesEpes.verificar(xml, otro.certificado())).isFalse();
    }

    private static Factura factura() {
        Factura f = mock(Factura.class);
        when(f.getNumeroCompleto()).thenReturn("SETT-15");
        when(f.getTotal()).thenReturn(new BigDecimal("119000"));
        when(f.getFechaEmision()).thenReturn(OffsetDateTime.parse("2026-11-20T10:15:00-05:00"));
        when(f.getMoneda()).thenReturn("COP");
        return f;
    }
}
