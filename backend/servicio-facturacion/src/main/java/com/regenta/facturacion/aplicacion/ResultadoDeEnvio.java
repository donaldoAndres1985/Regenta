package com.regenta.facturacion.aplicacion;

import java.util.List;

/** Confirmación de que la factura salió al cliente, con lo que se adjuntó. */
public record ResultadoDeEnvio(boolean enviado, String destinatario, List<String> adjuntos) {
}
