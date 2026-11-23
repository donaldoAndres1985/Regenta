package com.regenta.facturacion.infrastructure;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.cert.X509Certificate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

import javax.xml.XMLConstants;
import javax.xml.crypto.dom.DOMStructure;
import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.SignedInfo;
import javax.xml.crypto.dsig.Transform;
import javax.xml.crypto.dsig.XMLObject;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.regenta.facturacion.application.CertificadoDeFirma;
import com.regenta.facturacion.application.FirmadorDeXml;
import com.regenta.facturacion.domain.Factura;

/**
 * HU-116 C2 · Firma la factura con XAdES-EPES, como lo pide la DIAN: firma XML (RSA-SHA256,
 * resúmenes SHA-256, C14N) dentro de {@code ext:UBLExtensions}, con tres referencias (el documento
 * con la transformación enveloped, el {@code KeyInfo} y las {@code SignedProperties}), y en las
 * propiedades firmadas la hora de firma, el certificado firmante (su resumen y emisor/serie), la
 * política de firma de la DIAN con su hash y el rol {@code supplier}.
 *
 * <p>Usa la API XML Digital Signature de Java, sin librerías aparte. El XML de la factura es mínimo
 * (UBL 2.1 con número, CUFE, fecha y total): el UBL completo y el CUFE oficial, que necesita la
 * clave técnica de la resolución, son de HU-117.
 */
@Component
public class FirmadorXadesEpes implements FirmadorDeXml {

    static final String DS = XMLSignature.XMLNS;
    static final String XADES = "http://uri.etsi.org/01903/v1.3.2#";
    static final String UBL = "urn:oasis:names:specification:ubl:schema:xsd:Invoice-2";
    static final String CBC = "urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2";
    static final String CAC = "urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2";
    static final String EXT = "urn:oasis:names:specification:ubl:schema:xsd:CommonExtensionComponents-2";
    /** La política de firma de la DIAN (v2) y su hash SHA-256, como los publica la DIAN. */
    static final String POLITICA = "https://facturaelectronica.dian.gov.co/politicadefirma/v2/politicadefirmav2.pdf";
    static final String HASH_POLITICA = "dMoMvtcG5aIzgYo0tIsSQeVJBDnUnfSOfBpxXrmor0Y=";
    static final String RSA_SHA256 = "http://www.w3.org/2001/04/xmldsig-more#rsa-sha256";
    static final String TIPO_SIGNED_PROPERTIES = "http://uri.etsi.org/01903#SignedProperties";

    @Override
    public FirmaDeFactura firmar(Factura factura, CertificadoDeFirma certificado) {
        try {
            String cufe = cufe(factura, certificado.certificado());
            Document doc = factura(factura, cufe);
            firmar(doc, certificado, OffsetDateTime.now(ZoneOffset.UTC));
            return new FirmaDeFactura(cufe, bytes(doc));
        } catch (Exception fallo) {
            throw new IllegalStateException("No se pudo firmar la factura " + factura.getNumeroCompleto(), fallo);
        }
    }

    /** CUFE provisional (SHA-384 de los datos clave, 96 hex como el real): el oficial es de HU-117. */
    private static String cufe(Factura f, X509Certificate cert) throws Exception {
        String semilla = f.getNumeroCompleto() + "|" + f.getTotal() + "|" + f.getFechaEmision() + "|"
                + cert.getSerialNumber();
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-384")
                .digest(semilla.getBytes(StandardCharsets.UTF_8)));
    }

    private static Document factura(Factura f, String cufe) throws Exception {
        Document doc = constructor().newDocument();
        Element invoice = doc.createElementNS(UBL, "Invoice");
        // Declarado en el DOM: si lo agregara el serializador al escribir, lo firmado no sería lo guardado.
        invoice.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns", UBL);
        invoice.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns:cbc", CBC);
        invoice.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns:cac", CAC);
        invoice.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns:ext", EXT);
        doc.appendChild(invoice);
        Element extensiones = hijo(doc, invoice, EXT, "ext:UBLExtensions");
        Element extension = hijo(doc, extensiones, EXT, "ext:UBLExtension");
        hijo(doc, extension, EXT, "ext:ExtensionContent");
        texto(doc, invoice, CBC, "cbc:UBLVersionID", "UBL 2.1");
        texto(doc, invoice, CBC, "cbc:ID", f.getNumeroCompleto());
        Element uuid = texto(doc, invoice, CBC, "cbc:UUID", cufe);
        uuid.setAttribute("schemeName", "CUFE-SHA384");
        OffsetDateTime emision = f.getFechaEmision() == null ? OffsetDateTime.now(ZoneOffset.UTC) : f.getFechaEmision();
        texto(doc, invoice, CBC, "cbc:IssueDate", emision.toLocalDate().toString());
        texto(doc, invoice, CBC, "cbc:IssueTime", emision.toOffsetTime().withNano(0).toString());
        Element total = hijo(doc, invoice, CAC, "cac:LegalMonetaryTotal");
        BigDecimal valor = f.getTotal() == null ? BigDecimal.ZERO : f.getTotal();
        Element pagar = texto(doc, total, CBC, "cbc:PayableAmount", valor.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString());
        pagar.setAttribute("currencyID", f.getMoneda() == null ? "COP" : f.getMoneda());
        return doc;
    }

    /** XAdES-EPES dentro de la última {@code ext:ExtensionContent}. */
    static void firmar(Document doc, CertificadoDeFirma certificado, OffsetDateTime cuando) throws Exception {
        X509Certificate cert = certificado.certificado();
        XMLSignatureFactory fabrica = XMLSignatureFactory.getInstance("DOM");
        String id = "xmldsig-" + UUID.randomUUID();
        String keyInfoId = id + "-keyinfo";
        String propsId = id + "-signedprops";

        DigestMethod sha256 = fabrica.newDigestMethod(DigestMethod.SHA256, null);
        Reference documento = fabrica.newReference("", sha256,
                List.of(fabrica.newTransform(Transform.ENVELOPED, (TransformParameterSpec) null)), null, id + "-ref0");
        Reference llave = fabrica.newReference("#" + keyInfoId, sha256);
        Reference propiedades = fabrica.newReference("#" + propsId, sha256, null, TIPO_SIGNED_PROPERTIES, null);
        SignedInfo firmado = fabrica.newSignedInfo(
                fabrica.newCanonicalizationMethod(CanonicalizationMethod.INCLUSIVE, (C14NMethodParameterSpec) null),
                fabrica.newSignatureMethod(RSA_SHA256, null), List.of(documento, llave, propiedades));
        KeyInfoFactory llaves = fabrica.getKeyInfoFactory();
        KeyInfo keyInfo = llaves.newKeyInfo(List.of(llaves.newX509Data(List.of(cert))), keyInfoId);

        Element calificadas = doc.createElementNS(XADES, "xades:QualifyingProperties");
        calificadas.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns:xades", XADES);
        calificadas.setAttribute("Target", "#" + id);
        Element firmadas = hijo(doc, calificadas, XADES, "xades:SignedProperties");
        firmadas.setAttribute("Id", propsId);
        firmadas.setIdAttribute("Id", true);
        Element deLaFirma = hijo(doc, firmadas, XADES, "xades:SignedSignatureProperties");
        texto(doc, deLaFirma, XADES, "xades:SigningTime", cuando.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        Element firmante = hijo(doc, hijo(doc, deLaFirma, XADES, "xades:SigningCertificate"), XADES, "xades:Cert");
        Element resumenCert = hijo(doc, firmante, XADES, "xades:CertDigest");
        metodoSha256(doc, resumenCert);
        texto(doc, resumenCert, DS, "ds:DigestValue",
                Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-256").digest(cert.getEncoded())));
        Element emisorSerie = hijo(doc, firmante, XADES, "xades:IssuerSerial");
        texto(doc, emisorSerie, DS, "ds:X509IssuerName", cert.getIssuerX500Principal().getName());
        texto(doc, emisorSerie, DS, "ds:X509SerialNumber", cert.getSerialNumber().toString());
        Element politica = hijo(doc, hijo(doc, deLaFirma, XADES, "xades:SignaturePolicyIdentifier"), XADES,
                "xades:SignaturePolicyId");
        Element idPolitica = hijo(doc, politica, XADES, "xades:SigPolicyId");
        texto(doc, idPolitica, XADES, "xades:Identifier", POLITICA);
        texto(doc, idPolitica, XADES, "xades:Description",
                "Política de firma para facturas electrónicas de la República de Colombia");
        Element hashPolitica = hijo(doc, politica, XADES, "xades:SigPolicyHash");
        metodoSha256(doc, hashPolitica);
        texto(doc, hashPolitica, DS, "ds:DigestValue", HASH_POLITICA);
        Element roles = hijo(doc, hijo(doc, deLaFirma, XADES, "xades:SignerRole"), XADES, "xades:ClaimedRoles");
        texto(doc, roles, XADES, "xades:ClaimedRole", "supplier");

        XMLObject objeto = fabrica.newXMLObject(List.of(new DOMStructure(calificadas)), null, null, null);
        XMLSignature firma = fabrica.newXMLSignature(firmado, keyInfo, List.of(objeto), id, id + "-sigvalue");

        NodeList contenidos = doc.getElementsByTagNameNS(EXT, "ExtensionContent");
        Element donde = (Element) contenidos.item(contenidos.getLength() - 1);
        DOMSignContext contexto = new DOMSignContext(certificado.llave(), donde);
        contexto.setDefaultNamespacePrefix("ds");
        contexto.setIdAttributeNS(firmadas, null, "Id");
        firma.sign(contexto);
    }

    /**
     * C2 · ¿La firma del XML valida contra ese certificado? Valida la firma y cada referencia (el
     * documento, el KeyInfo y las propiedades firmadas): cualquier cambio en el XML la rompe.
     */
    public static boolean verificar(byte[] xml, X509Certificate certificado) {
        try {
            Document doc = constructor().parse(new ByteArrayInputStream(xml));
            NodeList firmas = doc.getElementsByTagNameNS(DS, "Signature");
            if (firmas.getLength() != 1) {
                return false;
            }
            DOMValidateContext contexto = new DOMValidateContext(certificado.getPublicKey(), firmas.item(0));
            registrarIds(contexto, doc.getElementsByTagNameNS(XADES, "SignedProperties"));
            registrarIds(contexto, doc.getElementsByTagNameNS(DS, "KeyInfo"));
            XMLSignature firma = XMLSignatureFactory.getInstance("DOM").unmarshalXMLSignature(contexto);
            return firma.validate(contexto);
        } catch (Exception ilegible) {
            return false;
        }
    }

    private static void registrarIds(DOMValidateContext contexto, NodeList elementos) {
        for (int i = 0; i < elementos.getLength(); i++) {
            Element e = (Element) elementos.item(i);
            if (e.hasAttribute("Id")) {
                contexto.setIdAttributeNS(e, null, "Id");
            }
        }
    }

    private static void metodoSha256(Document doc, Element padre) {
        Element m = hijo(doc, padre, DS, "ds:DigestMethod");
        m.setAttribute("Algorithm", DigestMethod.SHA256);
    }

    private static Element hijo(Document doc, Element padre, String ns, String nombre) {
        Element e = doc.createElementNS(ns, nombre);
        padre.appendChild(e);
        return e;
    }

    private static Element texto(Document doc, Element padre, String ns, String nombre, String valor) {
        Element e = hijo(doc, padre, ns, nombre);
        e.setTextContent(valor);
        return e;
    }

    /** Con espacios de nombres y sin DTD ni entidades externas. */
    static DocumentBuilder constructor() throws Exception {
        DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
        f.setNamespaceAware(true);
        f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        f.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        f.setExpandEntityReferences(false);
        return f.newDocumentBuilder();
    }

    private static byte[] bytes(Document doc) throws Exception {
        var t = TransformerFactory.newInstance().newTransformer();
        t.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        t.setOutputProperty(OutputKeys.INDENT, "no");
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        t.transform(new DOMSource(doc), new StreamResult(salida));
        return salida.toByteArray();
    }
}
