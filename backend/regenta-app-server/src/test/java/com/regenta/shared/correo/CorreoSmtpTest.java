package com.regenta.shared.correo;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetupTest;
import com.regenta.shared.correo.CorreoSaliente.Adjunto;
import com.regenta.shared.correo.CorreoSaliente.Correo;
import com.regenta.shared.correo.CorreoSaliente.ResultadoDeEnvio;

import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

/** HU-122 · El correo de verdad por SMTP, contra un servidor SMTP de mentira (GreenMail). */
class CorreoSmtpTest {

    @RegisterExtension
    static final GreenMailExtension SMTP = new GreenMailExtension(ServerSetupTest.SMTP);

    private final UUID negocio = UUID.randomUUID();
    private final RemitenteDelNegocio remitentes =
            id -> new RemitenteDelNegocio.Remitente("Ferretería Central", "ventas@ferreteriacentral.co");

    private CorreoSmtp correo(int puerto) {
        JavaMailSenderImpl smtp = new JavaMailSenderImpl();
        smtp.setHost("127.0.0.1");
        smtp.setPort(puerto);
        smtp.getJavaMailProperties().put("mail.smtp.connectiontimeout", "2000");
        smtp.getJavaMailProperties().put("mail.smtp.timeout", "2000");
        return new CorreoSmtp(smtp, "notificaciones@regenta.app", remitentes);
    }

    @Test
    @DisplayName("C1 · Sale por el SMTP, con el nombre del negocio y su correo para responder; queda el id del mensaje")
    void sale() throws Exception {
        ResultadoDeEnvio r = correo(SMTP.getSmtp().getPort())
                .enviar(new Correo(negocio, "contador@cliente.co", "Factura SETT-1", "Hola, adjunto va la factura."));

        assertThat(r.enviado()).isTrue();
        MimeMessage[] recibidos = SMTP.getReceivedMessages();
        assertThat(recibidos).hasSize(1);
        MimeMessage m = recibidos[0];
        assertThat(r.idProveedor()).isEqualTo(m.getMessageID());
        InternetAddress de = (InternetAddress) m.getFrom()[0];
        assertThat(de.getAddress()).isEqualTo("notificaciones@regenta.app");
        assertThat(de.getPersonal()).isEqualTo("Ferretería Central");
        assertThat(((InternetAddress) m.getReplyTo()[0]).getAddress()).isEqualTo("ventas@ferreteriacentral.co");
        assertThat(m.getSubject()).isEqualTo("Factura SETT-1");
        assertThat(GreenMailUtil.getBody(m)).contains("adjunto va la factura");
    }

    @Test
    @DisplayName("C3 · El archivo llega adjunto, con su nombre y su contenido, no como enlace")
    void adjunto() throws Exception {
        byte[] xlsx = {80, 75, 3, 4, 20, 0};
        correo(SMTP.getSmtp().getPort()).enviar(new Correo(negocio, "contador@cliente.co", "Ventas de noviembre",
                "Adjunto va el reporte.", List.of(new Adjunto("ventas.xlsx",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", xlsx))));

        Multipart partes = (Multipart) SMTP.getReceivedMessages()[0].getContent();
        Part adjunto = null;
        for (int i = 0; i < partes.getCount(); i++) {
            if (Part.ATTACHMENT.equalsIgnoreCase(partes.getBodyPart(i).getDisposition())) {
                adjunto = partes.getBodyPart(i);
            }
        }
        assertThat(adjunto).isNotNull();
        assertThat(adjunto.getFileName()).isEqualTo("ventas.xlsx");
        assertThat(adjunto.getInputStream().readAllBytes()).isEqualTo(xlsx);
    }

    @Test
    @DisplayName("C2 · Una dirección que el servidor rechaza no tumba nada: vuelve el motivo para reintentar")
    void rechazo() {
        ResultadoDeEnvio r = correo(SMTP.getSmtp().getPort())
                .enviar(new Correo(negocio, "esto no es un correo", "Factura", "Hola"));

        assertThat(r.enviado()).isFalse();
        assertThat(r.motivo()).isNotBlank();
        assertThat(SMTP.getReceivedMessages()).isEmpty();
    }

    @Test
    @DisplayName("C4 · Con el proveedor caído no se lanza nada: vuelve el motivo y quien llama reintenta")
    void proveedorCaido() {
        ResultadoDeEnvio r = correo(1).enviar(new Correo(negocio, "contador@cliente.co", "Factura", "Hola"));

        assertThat(r.enviado()).isFalse();
        assertThat(r.motivo()).isNotBlank();
    }

    @Test
    @DisplayName("Sin negocio (o sin correo configurado) sale como Regenta y sin Reply-To")
    void sinNegocio() throws Exception {
        JavaMailSenderImpl smtp = new JavaMailSenderImpl();
        smtp.setHost("127.0.0.1");
        smtp.setPort(SMTP.getSmtp().getPort());
        new CorreoSmtp(smtp, "notificaciones@regenta.app", null)
                .enviar(new Correo(null, "admin@negocio.co", "Alerta", "Stock bajo"));

        MimeMessage m = SMTP.getReceivedMessages()[0];
        assertThat(((InternetAddress) m.getFrom()[0]).getPersonal()).isEqualTo("Regenta");
        assertThat(((InternetAddress) m.getReplyTo()[0]).getAddress()).isEqualTo("notificaciones@regenta.app");
        assertThat(new String(m.getSubject().getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8)).isEqualTo("Alerta");
    }
}
