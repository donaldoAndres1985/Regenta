package com.regenta.facturacion.infra;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.regenta.facturacion.aplicacion.EnviadorDeCorreo;

/**
 * Stub del envío de correo. Guarda el último envío para las pruebas. La
 * integración real (SMTP) reemplaza este bean.
 */
@Component
public class EnviadorDeCorreoStub implements EnviadorDeCorreo {

    public volatile UUID ultimoNegocio;
    public volatile String ultimoNumero;
    public volatile String ultimoDestinatario;
    public volatile List<String> ultimosAdjuntos;

    @Override
    public void enviar(UUID negocioId, String numeroCompleto, String destinatario,
            List<String> adjuntos) {
        this.ultimoNegocio = negocioId;
        this.ultimoNumero = numeroCompleto;
        this.ultimoDestinatario = destinatario;
        this.ultimosAdjuntos = adjuntos;
    }
}
