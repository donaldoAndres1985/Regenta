package com.regenta.facturacion.aplicacion;

import java.util.List;
import java.util.UUID;

/**
 * Manda la factura al cliente por correo con el PDF y el XML adjuntos. La
 * implementación real (SMTP / proveedor) reemplaza este bean; en pruebas se
 * sustituye.
 */
public interface EnviadorDeCorreo {

    void enviar(UUID negocioId, String numeroCompleto, String destinatario, List<String> adjuntos);
}
