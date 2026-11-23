package com.regenta.facturacion.infrastructure;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.xml.XMLConstants;
import javax.xml.crypto.dom.DOMStructure;
import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.SignedInfo;
import javax.xml.crypto.dsig.Transform;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.spec.ExcC14NParameterSpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.regenta.facturacion.application.CertificadoDeFirma;
import com.regenta.facturacion.application.ClienteDeLaDian;
import com.regenta.facturacion.application.DianNoDisponibleException;
import com.regenta.facturacion.application.ParametrosDeLaDian;

/**
 * HU-117 · El cliente del servicio web de la DIAN ({@code WcfDianCustomerServices}, anexo técnico 1.9,
 * capítulo 7). SOAP 1.2 con WS-Security: {@code Timestamp}, el certificado como
 * {@code BinarySecurityToken} y una firma RSA-SHA256 sobre {@code wsa:To} con C14N exclusiva y los
 * prefijos {@code wsa soap wcf} incluidos, que es la forma que acepta el servidor de la DIAN.
 *
 * <p>La factura va en un ZIP ({@code z….zip} con {@code fv….xml}). Con un {@code DIAN_TEST_SET_ID}
 * configurado se envía al set de pruebas ({@code SendTestSetAsync}) y se consulta el resultado con
 * {@code GetStatusZip}; sin él, {@code SendBillSync}. Habilitación y producción son solo URL y ambiente
 * (C5).
 *
 * <p>No hubo respuesta (red, tiempo agotado, 5xx sin SOAP, set de pruebas que no termina):
 * {@link DianNoDisponibleException}, y la factura se reintenta hasta la contingencia (C3).
 */
@Component
@ConditionalOnExpression("'${regenta.dian.url:}' != ''")
public class ClienteDeLaDianSoap implements ClienteDeLaDian {

    private static final Logger LOG = LoggerFactory.getLogger(ClienteDeLaDianSoap.class);
    static final String SOAP = "http://www.w3.org/2003/05/soap-envelope";
    static final String WCF = "http://wcf.dian.colombia";
    static final String WSA = "http://www.w3.org/2005/08/addressing";
    static final String WSSE = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd";
    static final String WSU = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd";
    static final String X509 = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-x509-token-profile-1.0#X509v3";
    static final String BASE64 = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#Base64Binary";
    static final String ACCION = "http://wcf.dian.colombia/IWcfDianCustomerServices/";

    private final ParametrosDeLaDian parametros;
    private final HttpClient http;
    private final Duration espera;
    private final int consultasDelSet;
    private final Duration pausaEntreConsultas;

    public ClienteDeLaDianSoap(ParametrosDeLaDian parametros,
            @Value("${regenta.dian.timeout-segundos:30}") int timeoutSegundos,
            @Value("${regenta.dian.consultas-set-de-pruebas:6}") int consultasDelSet,
            @Value("${regenta.dian.pausa-consultas-ms:5000}") long pausaMs) {
        if (parametros.softwareId().isEmpty() || parametros.pin().isEmpty()) {
            throw new IllegalStateException("Con DIAN_URL configurada faltan DIAN_SOFTWARE_ID y DIAN_SOFTWARE_PIN");
        }
        this.parametros = parametros;
        this.espera = Duration.ofSeconds(timeoutSegundos);
        this.consultasDelSet = Math.max(1, consultasDelSet);
        this.pausaEntreConsultas = Duration.ofMillis(Math.max(0, pausaMs));
        this.http = HttpClient.newBuilder().connectTimeout(espera).version(HttpClient.Version.HTTP_1_1).build();
        LOG.info("DIAN: {} (ambiente {}{})", parametros.url(), parametros.ambiente(),
                parametros.enSetDePruebas() ? ", set de pruebas" : "");
    }

    @Override
    public RespuestaDeLaDian transmitir(EnvioALaDian envio) {
        long inicio = System.currentTimeMillis();
        String nombreZip = "z" + envio.nombreArchivo() + ".zip";
        String contenido = Base64.getEncoder().encodeToString(zip("fv" + envio.nombreArchivo() + ".xml",
                envio.xmlFirmado()));
        if (!parametros.enSetDePruebas()) {
            String request = sobre("SendBillSync", envio.certificado(), cuerpo -> {
                texto(cuerpo, "wcf:fileName", nombreZip);
                texto(cuerpo, "wcf:contentFile", contenido);
            });
            Llamada r = llamar("SendBillSync", request);
            return respuesta(primero(r.doc(), "SendBillSyncResult"), r, request, inicio);
        }

        String request = sobre("SendTestSetAsync", envio.certificado(), cuerpo -> {
            texto(cuerpo, "wcf:fileName", nombreZip);
            texto(cuerpo, "wcf:contentFile", contenido);
            texto(cuerpo, "wcf:testSetId", parametros.testSetId());
        });
        Llamada enviado = llamar("SendTestSetAsync", request);
        Element resultado = primero(enviado.doc(), "SendTestSetAsyncResult");
        String zipKey = valor(resultado, "ZipKey");
        if (zipKey.isEmpty()) {
            List<String> errores = lista(resultado, "ErrorMessageList");
            return new RespuestaDeLaDian(false, enviado.status(), "99", "La DIAN no recibió el set de pruebas", null,
                    errores, request, enviado.cuerpo(), System.currentTimeMillis() - inicio);
        }
        for (int i = 0; i < consultasDelSet; i++) {
            pausa();
            String consulta = sobre("GetStatusZip", envio.certificado(), cuerpo -> texto(cuerpo, "wcf:trackId", zipKey));
            Llamada estado = llamar("GetStatusZip", consulta);
            Element dian = primero(estado.doc(), "DianResponse");
            String codigo = valor(dian, "StatusCode");
            // 98: en proceso. Se vuelve a preguntar.
            if (dian != null && !codigo.isEmpty() && !"98".equals(codigo)) {
                return respuesta(dian, estado, request, inicio);
            }
        }
        throw new DianNoDisponibleException("El set de pruebas " + zipKey + " sigue en proceso en la DIAN");
    }

    private RespuestaDeLaDian respuesta(Element dian, Llamada r, String request, long inicio) {
        if (dian == null) {
            throw new DianNoDisponibleException("La DIAN respondió sin resultado (HTTP " + r.status() + ")");
        }
        boolean valida = "true".equalsIgnoreCase(valor(dian, "IsValid"));
        String codigo = valor(dian, "StatusCode");
        String mensaje = valor(dian, "StatusMessage");
        if (mensaje.isEmpty()) {
            mensaje = valor(dian, "StatusDescription");
        }
        List<String> errores = lista(dian, "ErrorMessage");
        return new RespuestaDeLaDian(valida, r.status(), valida ? null : (codigo.isEmpty() ? "99" : codigo),
                valida ? mensaje : valor(dian, "StatusDescription"), valor(dian, "XmlDocumentKey"), errores, request,
                r.cuerpo(), System.currentTimeMillis() - inicio);
    }

    private record Llamada(int status, String cuerpo, Document doc) {
    }

    private Llamada llamar(String operacion, String sobre) {
        HttpRequest pedido = HttpRequest.newBuilder(URI.create(parametros.url()))
                .timeout(espera)
                .header("Content-Type", "application/soap+xml;charset=UTF-8;action=\"" + ACCION + operacion + "\"")
                .POST(HttpRequest.BodyPublishers.ofString(sobre, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> r;
        try {
            r = http.send(pedido, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (HttpTimeoutException agotado) {
            throw new DianNoDisponibleException("La DIAN no respondió en " + espera.toSeconds() + " s", agotado);
        } catch (IOException red) {
            throw new DianNoDisponibleException("No se pudo hablar con la DIAN: " + red.getMessage(), red);
        } catch (InterruptedException cortado) {
            Thread.currentThread().interrupt();
            throw new DianNoDisponibleException("Se interrumpió el envío a la DIAN", cortado);
        }
        Document doc = leer(r.body());
        if (doc == null || primero(doc, "Body") == null) {
            throw new DianNoDisponibleException("La DIAN respondió HTTP " + r.statusCode() + " sin un mensaje SOAP");
        }
        Element falla = primero(doc, "Fault");
        if (falla != null) {
            // Un SOAP Fault no es un rechazo de la factura: es el mensaje (seguridad, formato). Se registra
            // y se reintenta; el texto queda en el log de transmisiones.
            throw new DianNoDisponibleException("La DIAN devolvió un error SOAP: " + falla.getTextContent().trim());
        }
        return new Llamada(r.statusCode(), r.body(), doc);
    }

    /** El sobre SOAP 1.2 de una operación, con su encabezado WS-Addressing y WS-Security firmado. */
    String sobre(String operacion, CertificadoDeFirma certificado, java.util.function.Consumer<Element> cuerpo) {
        try {
            Document doc = FirmadorXadesEpes.constructor().newDocument();
            Element envelope = doc.createElementNS(SOAP, "soap:Envelope");
            envelope.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns:soap", SOAP);
            envelope.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns:wcf", WCF);
            doc.appendChild(envelope);
            Element header = hijo(envelope, SOAP, "soap:Header");
            header.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns:wsa", WSA);

            Element seguridad = hijo(header, WSSE, "wsse:Security");
            seguridad.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns:wsse", WSSE);
            seguridad.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns:wsu", WSU);
            Instant ahora = Instant.now().truncatedTo(ChronoUnit.MILLIS);
            Element sello = hijo(seguridad, WSU, "wsu:Timestamp");
            sello.setAttributeNS(WSU, "wsu:Id", "TS-" + UUID.randomUUID());
            hijo(sello, WSU, "wsu:Created").setTextContent(ahora.toString());
            hijo(sello, WSU, "wsu:Expires").setTextContent(ahora.plus(Duration.ofMinutes(5)).toString());
            String tokenId = "X509-" + UUID.randomUUID();
            Element token = hijo(seguridad, WSSE, "wsse:BinarySecurityToken");
            token.setAttribute("EncodingType", BASE64);
            token.setAttribute("ValueType", X509);
            token.setAttributeNS(WSU, "wsu:Id", tokenId);
            token.setTextContent(Base64.getEncoder().encodeToString(certificado.certificado().getEncoded()));

            hijo(header, WSA, "wsa:Action").setTextContent(ACCION + operacion);
            Element para = hijo(header, WSA, "wsa:To");
            // Cada elemento con wsu:Id declara wsu: la DIAN rechaza el mensaje si lo hereda.
            para.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns:wsu", WSU);
            String paraId = "id-" + UUID.randomUUID();
            para.setAttributeNS(WSU, "wsu:Id", paraId);
            para.setTextContent(parametros.url());

            Element body = hijo(envelope, SOAP, "soap:Body");
            Element op = hijo(body, WCF, "wcf:" + operacion);
            cuerpo.accept(op);

            firmarEncabezado(doc, seguridad, para, paraId, token, tokenId, certificado);
            return FirmadorXadesEpes.texto(doc);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception fallo) {
            throw new IllegalStateException("No se pudo armar el mensaje para la DIAN", fallo);
        }
    }

    private static void firmarEncabezado(Document doc, Element seguridad, Element para, String paraId, Element token,
            String tokenId, CertificadoDeFirma certificado) throws Exception {
        XMLSignatureFactory fabrica = XMLSignatureFactory.getInstance("DOM");
        Reference referencia = fabrica.newReference("#" + paraId,
                fabrica.newDigestMethod(DigestMethod.SHA256, null),
                List.of(fabrica.newTransform(CanonicalizationMethod.EXCLUSIVE,
                        new ExcC14NParameterSpec(List.of("soap", "wcf")))),
                null, null);
        SignedInfo firmado = fabrica.newSignedInfo(
                fabrica.newCanonicalizationMethod(CanonicalizationMethod.EXCLUSIVE,
                        new ExcC14NParameterSpec(List.of("wsa", "soap", "wcf"))),
                fabrica.newSignatureMethod(FirmadorXadesEpes.RSA_SHA256, null), List.of(referencia));

        Element str = doc.createElementNS(WSSE, "wsse:SecurityTokenReference");
        Element ref = doc.createElementNS(WSSE, "wsse:Reference");
        ref.setAttribute("URI", "#" + tokenId);
        ref.setAttribute("ValueType", X509);
        str.appendChild(ref);
        KeyInfoFactory llaves = fabrica.getKeyInfoFactory();
        KeyInfo keyInfo = llaves.newKeyInfo(List.of(new DOMStructure(str)), "KI-" + UUID.randomUUID());

        XMLSignature firma = fabrica.newXMLSignature(firmado, keyInfo, null, "SIG-" + UUID.randomUUID(), null);
        DOMSignContext contexto = new DOMSignContext(certificado.llave(), seguridad);
        contexto.setDefaultNamespacePrefix("ds");
        contexto.setIdAttributeNS(para, WSU, "Id");
        contexto.setIdAttributeNS(token, WSU, "Id");
        firma.sign(contexto);
    }

    private void pausa() {
        if (pausaEntreConsultas.isZero()) {
            return;
        }
        try {
            Thread.sleep(pausaEntreConsultas.toMillis());
        } catch (InterruptedException cortado) {
            Thread.currentThread().interrupt();
            throw new DianNoDisponibleException("Se interrumpió la consulta a la DIAN", cortado);
        }
    }

    static byte[] zip(String nombre, byte[] contenido) {
        try {
            ByteArrayOutputStream salida = new ByteArrayOutputStream();
            try (ZipOutputStream z = new ZipOutputStream(salida)) {
                z.putNextEntry(new ZipEntry(nombre));
                z.write(contenido);
                z.closeEntry();
            }
            return salida.toByteArray();
        } catch (IOException imposible) {
            throw new IllegalStateException(imposible);
        }
    }

    private static Document leer(String xml) {
        if (xml == null || xml.isBlank()) {
            return null;
        }
        try {
            return FirmadorXadesEpes.constructor().parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception noEsXml) {
            return null;
        }
    }

    private static Element hijo(Element padre, String ns, String nombre) {
        Element e = padre.getOwnerDocument().createElementNS(ns, nombre);
        padre.appendChild(e);
        return e;
    }

    private static void texto(Element padre, String nombre, String valor) {
        hijo(padre, WCF, nombre).setTextContent(valor);
    }

    /** El primer elemento con ese nombre local, sin importar el prefijo que use la DIAN. */
    static Element primero(Node raiz, String local) {
        if (raiz == null) {
            return null;
        }
        NodeList todos = raiz instanceof Document d ? d.getElementsByTagNameNS("*", local)
                : ((Element) raiz).getElementsByTagNameNS("*", local);
        return todos.getLength() == 0 ? null : (Element) todos.item(0);
    }

    static String valor(Element padre, String local) {
        Element e = primero(padre, local);
        return e == null ? "" : e.getTextContent().trim();
    }

    /** Las cadenas de una lista ({@code <c:string>…</c:string>}). */
    static List<String> lista(Element padre, String local) {
        List<String> salida = new ArrayList<>();
        Element contenedor = primero(padre, local);
        if (contenedor == null) {
            return salida;
        }
        NodeList hijos = contenedor.getChildNodes();
        for (int i = 0; i < hijos.getLength(); i++) {
            if (hijos.item(i) instanceof Element e && !e.getTextContent().isBlank()) {
                salida.add(e.getTextContent().trim());
            }
        }
        return salida;
    }
}
