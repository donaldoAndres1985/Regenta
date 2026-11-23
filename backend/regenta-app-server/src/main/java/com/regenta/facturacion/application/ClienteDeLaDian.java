package com.regenta.facturacion.application;

import java.util.List;

/**
 * Transmite el XML firmado a la DIAN y devuelve su respuesta. Lanza {@link DianNoDisponibleException} si
 * no hubo respuesta (red, caída, tiempo agotado): eso no es un rechazo, se reintenta (HU-057).
 */
public interface ClienteDeLaDian {

    RespuestaDeLaDian transmitir(EnvioALaDian envio);

    /**
     * Lo que se manda: el nombre base de los archivos (anexo 6.5.7, sin {@code fv}/{@code z} ni extensión),
     * el XML firmado y el certificado con que se firma el mensaje (WS-Security).
     */
    record EnvioALaDian(String nombreArchivo, byte[] xmlFirmado, CertificadoDeFirma certificado) {
    }

    /**
     * Lo que respondió la DIAN: si la aceptó, el código de estado, el CUFE que devolvió y la lista de
     * reglas que reportó (rechazos y notificaciones), con el request y el response crudos para el log.
     */
    record RespuestaDeLaDian(
            boolean aceptada,
            int httpStatus,
            String codigoError,
            String mensaje,
            String cufe,
            List<String> errores,
            String request,
            String response,
            long duracionMs) {

        public RespuestaDeLaDian {
            errores = errores == null ? List.of() : List.copyOf(errores);
        }
    }
}
