package com.regenta.facturacion.aplicacion;

/**
 * Transmite el XML firmado a la DIAN y devuelve su respuesta. Lanza
 * {@link DianNoDisponibleException} si no hubo respuesta (red / caída).
 */
public interface ClienteDeLaDian {

    RespuestaDeLaDian transmitir(byte[] xmlFirmado, String ambiente);

    /** Lo que respondió la DIAN, con el request y el response crudos para el log. */
    record RespuestaDeLaDian(
            boolean aceptada,
            int httpStatus,
            String codigoError,
            String mensaje,
            String request,
            String response,
            long duracionMs) {
    }
}
