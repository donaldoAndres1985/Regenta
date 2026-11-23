package com.regenta.shared.correo;

import java.io.UnsupportedEncodingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

/**
 * HU-122 · El correo de verdad, por SMTP: sirve con cualquier proveedor (SES, SendGrid, Brevo,
 * Mailgun, Gmail de empresa…) con sus credenciales por entorno. Sale desde la dirección de la
 * plataforma ({@code CORREO_REMITENTE}, la que el proveedor tiene verificada), con el nombre visible
 * del negocio y respondiendo a su correo.
 */
public class CorreoSmtp implements CorreoSaliente {

    private static final Logger LOG = LoggerFactory.getLogger(CorreoSmtp.class);

    private final JavaMailSender smtp;
    private final String direccion;
    private final RemitenteDelNegocio remitentes;

    public CorreoSmtp(JavaMailSender smtp, String direccion, RemitenteDelNegocio remitentes) {
        this.smtp = smtp;
        this.direccion = direccion;
        this.remitentes = remitentes;
    }

    @Override
    public ResultadoDeEnvio enviar(Correo correo) {
        if (correo.para() == null || correo.para().isBlank()) {
            return ResultadoDeEnvio.fallo("No hay a quién enviar el correo");
        }
        try {
            MimeMessage mensaje = smtp.createMimeMessage();
            MimeMessageHelper armado = new MimeMessageHelper(mensaje, !correo.adjuntos().isEmpty(), "UTF-8");
            RemitenteDelNegocio.Remitente remitente = remitentes == null || correo.negocioId() == null
                    ? new RemitenteDelNegocio.Remitente("Regenta", null)
                    : remitentes.de(correo.negocioId());
            armado.setFrom(new InternetAddress(direccion, remitente.nombre(), "UTF-8"));
            if (remitente.responderA() != null && !remitente.responderA().isBlank()) {
                armado.setReplyTo(remitente.responderA());
            }
            armado.setTo(correo.para().trim());
            armado.setSubject(correo.asunto() == null ? "" : correo.asunto());
            armado.setText(correo.cuerpo() == null ? "" : correo.cuerpo(), false);
            for (Adjunto a : correo.adjuntos()) {
                armado.addAttachment(a.nombre(), new ByteArrayResource(a.contenido()),
                        a.tipoMime() == null ? "application/octet-stream" : a.tipoMime());
            }
            mensaje.saveChanges();
            String id = mensaje.getMessageID();
            smtp.send(mensaje);
            return ResultadoDeEnvio.ok(id);
        } catch (MailException | MessagingException | UnsupportedEncodingException fallo) {
            // Rebote, rechazo o proveedor caído: el motivo queda para el reintento (C2, C4).
            String motivo = motivo(fallo);
            LOG.warn("No salió el correo a {}: {}", correo.para(), motivo);
            return ResultadoDeEnvio.fallo(motivo);
        }
    }

    private static String motivo(Throwable fallo) {
        Throwable raiz = fallo;
        while (raiz.getCause() != null && raiz.getCause() != raiz) {
            raiz = raiz.getCause();
        }
        String texto = raiz.getMessage() == null ? fallo.getMessage() : raiz.getMessage();
        texto = texto == null ? fallo.getClass().getSimpleName() : texto.strip();
        return texto.length() > 500 ? texto.substring(0, 500) : texto;
    }
}
