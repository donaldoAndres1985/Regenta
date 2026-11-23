package com.regenta.facturacion.infrastructure;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Deque;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMValidateContext;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

/**
 * HU-117 · La DIAN simulada por contrato: un servidor HTTP local que responde como
 * {@code WcfDianCustomerServices} (anexo técnico 1.9, capítulo 7) y revisa lo que le llega como lo haría
 * la DIAN: la acción SOAP, la firma WS-Security sobre {@code wsa:To} con el certificado del
 * {@code BinarySecurityToken} y el ZIP con la factura.
 */
public final class DianSimulada implements AutoCloseable {

    /** Lo que recibió en cada llamada. */
    public record Pedido(String accion, String cuerpo, boolean firmaValida, String archivoEnElZip, byte[] xml) {
    }

    private final HttpServer servidor;
    private final Deque<String> respuestas = new ArrayDeque<>();
    public final List<Pedido> pedidos = new ArrayList<>();
    public volatile long demoraMs = 0;

    public DianSimulada() throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/WcfDianCustomerServices.svc", this::atender);
        servidor.start();
    }

    public String url() {
        return "http://127.0.0.1:" + servidor.getAddress().getPort() + "/WcfDianCustomerServices.svc";
    }

    /** Encola la próxima respuesta (cuerpo SOAP completo; {@code null} = HTTP 500 vacío). */
    public DianSimulada luego(String sobre) {
        respuestas.add(sobre == null ? "" : sobre);
        return this;
    }

    private void atender(HttpExchange e) throws IOException {
        String cuerpo = new String(e.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        String tipo = e.getRequestHeaders().getFirst("Content-Type");
        String accion = tipo == null || !tipo.contains("action=") ? ""
                : tipo.substring(tipo.indexOf("action=") + 7).replace("\"", "");
        synchronized (pedidos) {
            pedidos.add(revisar(accion, cuerpo));
        }
        if (demoraMs > 0) {
            try {
                Thread.sleep(demoraMs);
            } catch (InterruptedException cortado) {
                Thread.currentThread().interrupt();
            }
        }
        String respuesta = respuestas.isEmpty() ? "" : respuestas.poll();
        int status = respuesta.isEmpty() ? 500 : respuesta.contains(":Fault>") ? 500 : 200;
        byte[] bytes = respuesta.getBytes(StandardCharsets.UTF_8);
        e.getResponseHeaders().add("Content-Type", "application/soap+xml; charset=utf-8");
        e.sendResponseHeaders(status, bytes.length == 0 ? -1 : bytes.length);
        if (bytes.length > 0) {
            try (OutputStream salida = e.getResponseBody()) {
                salida.write(bytes);
            }
        }
        e.close();
    }

    private static Pedido revisar(String accion, String cuerpo) {
        try {
            Document doc = FirmadorXadesEpes.constructor()
                    .parse(new ByteArrayInputStream(cuerpo.getBytes(StandardCharsets.UTF_8)));
            Element token = ClienteDeLaDianSoap.primero(doc, "BinarySecurityToken");
            X509Certificate cert = (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(
                    new ByteArrayInputStream(Base64.getMimeDecoder().decode(token.getTextContent())));
            Element firma = ClienteDeLaDianSoap.primero(doc, "Signature");
            DOMValidateContext ctx = new DOMValidateContext(cert.getPublicKey(), firma);
            Element para = ClienteDeLaDianSoap.primero(doc, "To");
            ctx.setIdAttributeNS(para, ClienteDeLaDianSoap.WSU, "Id");
            XMLSignature xs = XMLSignatureFactory.getInstance("DOM").unmarshalXMLSignature(ctx);
            Reference ref = (Reference) xs.getSignedInfo().getReferences().get(0);
            boolean valida = xs.validate(ctx) && ref.getURI().equals("#" + para.getAttributeNS(ClienteDeLaDianSoap.WSU, "Id"))
                    && ClienteDeLaDianSoap.primero(doc, "Timestamp") != null;

            String nombre = null;
            byte[] xml = null;
            String contenido = ClienteDeLaDianSoap.valor(doc.getDocumentElement(), "contentFile");
            if (!contenido.isEmpty()) {
                try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(Base64.getDecoder().decode(contenido)))) {
                    ZipEntry entrada = zip.getNextEntry();
                    nombre = entrada == null ? null : entrada.getName();
                    xml = zip.readAllBytes();
                }
            }
            return new Pedido(accion, cuerpo, valida, nombre, xml);
        } catch (Exception ilegible) {
            return new Pedido(accion, cuerpo, false, null, null);
        }
    }

    @Override
    public void close() {
        servidor.stop(0);
    }

    // ---- Respuestas de la DIAN, como las documenta el anexo técnico ----

    private static String sobre(String operacion, String resultado) {
        return "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:a=\"http://www.w3.org/2005/08/addressing\">"
                + "<s:Header><a:Action s:mustUnderstand=\"1\">http://wcf.dian.colombia/IWcfDianCustomerServices/"
                + operacion + "Response</a:Action></s:Header><s:Body><" + operacion
                + "Response xmlns=\"http://wcf.dian.colombia\">" + resultado + "</" + operacion + "Response></s:Body></s:Envelope>";
    }

    private static String dianResponse(boolean valida, String codigo, String descripcion, String mensaje, String cufe,
            List<String> errores) {
        StringBuilder e = new StringBuilder("<b:ErrorMessage xmlns:c=\"http://schemas.microsoft.com/2003/10/Serialization/Arrays\">");
        errores.forEach(x -> e.append("<c:string>").append(x).append("</c:string>"));
        e.append("</b:ErrorMessage>");
        return e + "<b:IsValid>" + valida + "</b:IsValid><b:StatusCode>" + codigo + "</b:StatusCode>"
                + "<b:StatusDescription>" + descripcion + "</b:StatusDescription><b:StatusMessage>" + mensaje
                + "</b:StatusMessage><b:XmlBase64Bytes/><b:XmlDocumentKey>" + cufe + "</b:XmlDocumentKey>"
                + "<b:XmlFileName>fv</b:XmlFileName>";
    }

    private static final String NS_B = " xmlns:b=\"http://schemas.datacontract.org/2004/07/DianResponse\""
            + " xmlns:i=\"http://www.w3.org/2001/XMLSchema-instance\"";

    public static String aceptada(String cufe, String... notificaciones) {
        return sobre("SendBillSync", "<SendBillSyncResult" + NS_B + ">" + dianResponse(true, "00",
                "Procesado Correctamente.", "La Factura electrónica SETP990000123, ha sido autorizada.", cufe,
                List.of(notificaciones)) + "</SendBillSyncResult>");
    }

    public static String rechazada(String... reglas) {
        return sobre("SendBillSync", "<SendBillSyncResult" + NS_B + ">" + dianResponse(false, "99",
                "Validación contiene errores en campos mandatorios.", "Documento con errores en campos mandatorios.",
                "", List.of(reglas)) + "</SendBillSyncResult>");
    }

    public static String setRecibido(String zipKey) {
        return sobre("SendTestSetAsync", "<SendTestSetAsyncResult xmlns:b=\"http://schemas.datacontract.org/2004/07/"
                + "UploadDocumentResponse\" xmlns:i=\"http://www.w3.org/2001/XMLSchema-instance\">"
                + "<b:ErrorMessageList/><b:ZipKey>" + zipKey + "</b:ZipKey></SendTestSetAsyncResult>");
    }

    public static String estadoDelZip(boolean valida, String codigo, String cufe) {
        return sobre("GetStatusZip", "<GetStatusZipResult" + NS_B + "><b:DianResponse>"
                + dianResponse(valida, codigo, "98".equals(codigo) ? "En proceso de validación" : "Procesado Correctamente.",
                        "", cufe, List.of()) + "</b:DianResponse></GetStatusZipResult>");
    }

    public static String falla(String texto) {
        return "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\"><s:Body><s:Fault><s:Code><s:Value>"
                + "s:Sender</s:Value></s:Code><s:Reason><s:Text xml:lang=\"en-US\">" + texto
                + "</s:Text></s:Reason></s:Fault></s:Body></s:Envelope>";
    }
}
