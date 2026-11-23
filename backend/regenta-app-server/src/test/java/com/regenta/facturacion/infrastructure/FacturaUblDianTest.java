package com.regenta.facturacion.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

import com.regenta.facturacion.application.DocumentoAFirmar;
import com.regenta.facturacion.application.FacturaDePrueba;
import com.regenta.facturacion.application.ParametrosDeLaDian;
import com.regenta.facturacion.domain.Ambiente;
import com.regenta.facturacion.domain.Resolucion;
import com.regenta.facturacion.domain.TipoDocumento;
import com.regenta.shared.web.ReglaDeNegocioException;

/** HU-117 · El UBL 2.1 y el CUFE oficial, como los recalcula la DIAN. */
class FacturaUblDianTest {

    private final ParametrosDeLaDian pruebas = FacturaDePrueba.parametros("", "2", "");

    @Test
    void elCufeReproduceElEjemploDelAnexoTecnico() {
        // Anexo técnico 1.9, numeral 11.2: el ejemplo oficial.
        String cufe = FacturaUblDian.cufe("323200000129", "2019-01-16", "10:53:10-05:00", new BigDecimal("1500000"),
                new BigDecimal("285000"), BigDecimal.ZERO, null, new BigDecimal("1785000"), "700085371",
                "800199436", "693ff6f2a553c3646a063436fd4dd9ded0311471", "1");

        assertThat(cufe).isEqualTo("8bb918b19ba22a694f1da11c643b5e9de39adf60311cf179179e9b33381030bc"
                + "d4c3c3f156c506ed5908f9276f5bd9b4");
    }

    @Test
    void elDigitoDeVerificacionEsElDeLaDian() {
        assertThat(FacturaUblDian.digitoVerificacion("800197268")).isEqualTo("4");
        assertThat(FacturaUblDian.digitoVerificacion("900373076")).isEqualTo("4");
    }

    @Test
    void elCufeDelXmlEsElQueRecalculaLaDianDesdeElXml() throws Exception {
        FacturaUblDian.Construida ubl = FacturaUblDian.construir(FacturaDePrueba.documento(UUID.randomUUID()), pruebas);
        Document d = ubl.documento();

        String recalculado = FacturaUblDian.cufe(x(d, "/*[local-name()='Invoice']/*[local-name()='ID']"),
                x(d, "//*[local-name()='IssueDate']"), x(d, "//*[local-name()='IssueTime']"),
                new BigDecimal(x(d, "//*[local-name()='LegalMonetaryTotal']/*[local-name()='LineExtensionAmount']")),
                tributo(d, "01"), tributo(d, "04"), tributo(d, "03"),
                new BigDecimal(x(d, "//*[local-name()='LegalMonetaryTotal']/*[local-name()='PayableAmount']")),
                x(d, "//*[local-name()='AccountingSupplierParty']//*[local-name()='PartyTaxScheme']/*[local-name()='CompanyID']"),
                x(d, "//*[local-name()='AccountingCustomerParty']//*[local-name()='PartyTaxScheme']/*[local-name()='CompanyID']"),
                FacturaDePrueba.CLAVE_TECNICA, x(d, "//*[local-name()='ProfileExecutionID']"));

        assertThat(ubl.cufe()).hasSize(96).isEqualTo(recalculado)
                .isEqualTo(x(d, "/*[local-name()='Invoice']/*[local-name()='UUID']"));
    }

    @Test
    void totalesEImpuestosCuadranConLasLineas() throws Exception {
        Document d = FacturaUblDian.construir(FacturaDePrueba.documento(UUID.randomUUID()), pruebas).documento();

        // 2 x 25.000 con INC 8 % y 1 x 5.000 con IVA 19 %.
        assertThat(x(d, "//*[local-name()='LegalMonetaryTotal']/*[local-name()='LineExtensionAmount']"))
                .isEqualTo("55000.00");
        assertThat(tributo(d, "04")).isEqualByComparingTo("4000.00");
        assertThat(tributo(d, "01")).isEqualByComparingTo("950.00");
        assertThat(x(d, "//*[local-name()='LegalMonetaryTotal']/*[local-name()='TaxInclusiveAmount']"))
                .isEqualTo("59950.00");
        assertThat(x(d, "//*[local-name()='LegalMonetaryTotal']/*[local-name()='PayableAmount']")).isEqualTo("59950.00");
        assertThat(x(d, "count(//*[local-name()='InvoiceLine'])")).isEqualTo("2");
        assertThat(x(d, "//*[local-name()='LineCountNumeric']")).isEqualTo("2");
    }

    @Test
    void llevaLasExtensionesDianYDejaElLugarDeLaFirma() throws Exception {
        Document d = FacturaUblDian.construir(FacturaDePrueba.documento(UUID.randomUUID()), pruebas).documento();

        assertThat(x(d, "//*[local-name()='InvoiceAuthorization']")).isEqualTo("18760000001");
        assertThat(x(d, "//*[local-name()='AuthorizedInvoices']/*[local-name()='Prefix']")).isEqualTo("SETP");
        assertThat(x(d, "//*[local-name()='SoftwareID']")).isEqualTo("soft-123");
        assertThat(x(d, "//*[local-name()='SoftwareSecurityCode']"))
                .isEqualTo(FacturaUblDian.sha384("soft-123" + "75315" + "SETP990000123"));
        assertThat(x(d, "//*[local-name()='QRCode']")).contains("catalogo-vpfe-hab.dian.gov.co");
        assertThat(x(d, "count(//*[local-name()='UBLExtension'])")).isEqualTo("2");
        assertThat(x(d, "count(//*[local-name()='UBLExtension'][2]/*[local-name()='ExtensionContent']/*)")).isEqualTo("0");
        assertThat(x(d, "//*[local-name()='AccountingSupplierParty']//*[local-name()='CompanyID']/@schemeID"))
                .isEqualTo("4");
        assertThat(x(d, "//*[local-name()='AccountingSupplierParty']//*[local-name()='TaxLevelCode']"))
                .isEqualTo("O-13;O-15");
        assertThat(x(d, "//*[local-name()='AccountingSupplierParty']//*[local-name()='CountrySubentityCode']"))
                .isEqualTo("11");
    }

    @Test
    void c5_elAmbienteDeProduccionEsSoloConfiguracion() throws Exception {
        UUID negocio = UUID.randomUUID();
        DocumentoAFirmar doc = FacturaDePrueba.documento(negocio);
        FacturaUblDian.Construida enPruebas = FacturaUblDian.construir(doc, pruebas);
        FacturaUblDian.Construida enProduccion = FacturaUblDian.construir(doc,
                FacturaDePrueba.parametros("", "1", ""));

        assertThat(x(enProduccion.documento(), "//*[local-name()='ProfileExecutionID']")).isEqualTo("1");
        assertThat(x(enProduccion.documento(), "//*[local-name()='UUID']/@schemeID")).isEqualTo("1");
        assertThat(x(enProduccion.documento(), "//*[local-name()='QRCode']")).contains("https://catalogo-vpfe.dian.gov.co");
        assertThat(enProduccion.cufe()).isNotEqualTo(enPruebas.cufe());
    }

    @Test
    void sinClienteVaElConsumidorFinalDeLaDian() throws Exception {
        Map<String, Object> generico = Map.of("nombre", "Consumidor final", "tipo_documento", "13",
                "numero_documento", "222222222222", "tipo_organizacion", "2", "responsabilidad_fiscal", "R-99-PN",
                "pais", "CO", "generico", true);
        Document d = FacturaUblDian.construir(FacturaDePrueba.documento(UUID.randomUUID(), generico), pruebas)
                .documento();

        String cliente = "//*[local-name()='AccountingCustomerParty']";
        assertThat(x(d, cliente + "//*[local-name()='PartyTaxScheme']/*[local-name()='CompanyID']"))
                .isEqualTo("222222222222");
        assertThat(x(d, cliente + "//*[local-name()='PartyTaxScheme']/*[local-name()='CompanyID']/@schemeName"))
                .isEqualTo("13");
        assertThat(x(d, cliente + "//*[local-name()='RegistrationName']")).isEqualTo("Consumidor final");
    }

    @Test
    void sinClaveTecnicaNoHayCufe() {
        UUID negocio = UUID.randomUUID();
        DocumentoAFirmar doc = FacturaDePrueba.documento(negocio);
        Resolucion sinClave = Resolucion.cargar(negocio, null, TipoDocumento.FACTURA_VENTA, "18760000001", "SETP",
                990000000L, 995000000L, null, doc.resolucion().getVigenteDesde(), doc.resolucion().getVigenteHasta(),
                Ambiente.PRUEBAS);

        assertThatThrownBy(() -> FacturaUblDian.construir(
                new DocumentoAFirmar(doc.factura(), doc.lineas(), doc.impuestos(), sinClave), pruebas))
                .isInstanceOf(ReglaDeNegocioException.class).hasMessageContaining("clave técnica");
    }

    private static BigDecimal tributo(Document d, String codigo) throws Exception {
        String v = x(d, "/*[local-name()='Invoice']/*[local-name()='TaxTotal'][.//*[local-name()='TaxScheme']/"
                + "*[local-name()='ID']='" + codigo + "']/*[local-name()='TaxAmount']");
        return v.isEmpty() ? BigDecimal.ZERO : new BigDecimal(v);
    }

    static String x(Document d, String expresion) throws Exception {
        XPath xpath = XPathFactory.newInstance().newXPath();
        return xpath.evaluate(expresion, d).trim();
    }
}
