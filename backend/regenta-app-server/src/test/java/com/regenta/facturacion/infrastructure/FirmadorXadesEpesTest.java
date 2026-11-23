package com.regenta.facturacion.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.regenta.facturacion.application.CertificadoDeFirma;
import com.regenta.facturacion.application.CertificadoDePrueba;
import com.regenta.facturacion.application.FacturaDePrueba;
import com.regenta.facturacion.application.FirmadorDeXml.FirmaDeFactura;

/** HU-116 C2 · La firma XAdES-EPES del UBL completo valida contra el certificado, y solo contra ese. */
class FirmadorXadesEpesTest {

    private final FirmadorXadesEpes firmador = new FirmadorXadesEpes(FacturaDePrueba.parametros("", "2", ""));
    private final CertificadoDeFirma certificado =
            CertificadoDeFirma.abrir(CertificadoDePrueba.vigente("clave-1"), "clave-1");

    private FirmaDeFactura firmada() {
        return firmador.firmar(FacturaDePrueba.documento(UUID.randomUUID()), certificado);
    }

    @Test
    void c2_laFirmaValidaContraElCertificado() {
        FirmaDeFactura firma = firmada();

        assertThat(firma.cufe()).hasSize(96);
        assertThat(FirmadorXadesEpes.verificar(firma.xmlFirmado(), certificado.certificado())).isTrue();
    }

    @Test
    void c2_esXadesEpesEnLaSegundaExtensionUbl() {
        String xml = new String(firmada().xmlFirmado(), StandardCharsets.UTF_8);

        assertThat(xml)
                .contains("</sts:DianExtensions></ext:ExtensionContent></ext:UBLExtension><ext:UBLExtension>"
                        + "<ext:ExtensionContent><ds:Signature")
                .contains("xades:QualifyingProperties")
                .contains("xades:SigningTime")
                .contains("xades:SigningCertificate")
                .contains("xades:SignaturePolicyIdentifier")
                .contains(FirmadorXadesEpes.POLITICA)
                .contains("http://uri.etsi.org/01903#SignedProperties")
                .contains("rsa-sha256")
                .contains("<cbc:ID>SETP990000123</cbc:ID>");
    }

    @Test
    void c2_unXmlAlteradoYaNoValida() {
        byte[] xml = firmada().xmlFirmado();
        String alterado = new String(xml, StandardCharsets.UTF_8).replace("59950.00", "1.00");

        assertThat(FirmadorXadesEpes.verificar(alterado.getBytes(StandardCharsets.UTF_8),
                certificado.certificado())).isFalse();
    }

    @Test
    void c2_conOtroCertificadoNoValida() {
        byte[] xml = firmada().xmlFirmado();
        CertificadoDeFirma otro = CertificadoDeFirma.abrir(CertificadoDePrueba.vigente("otra"), "otra");

        assertThat(FirmadorXadesEpes.verificar(xml, otro.certificado())).isFalse();
    }
}
