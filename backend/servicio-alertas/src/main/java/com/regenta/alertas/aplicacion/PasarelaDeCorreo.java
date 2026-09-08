package com.regenta.alertas.aplicacion;

/**
 * El envío de correo de una alerta (HU-094). La implementación real habla con un
 * SMTP/SES; hasta entonces, un stub configurable la reemplaza.
 */
public interface PasarelaDeCorreo {

    ResultadoDeCorreo enviar(String destino, String titulo, String cuerpo);

    record ResultadoDeCorreo(String proveedorId, boolean exito, String error) {

        public static ResultadoDeCorreo ok(String proveedorId) {
            return new ResultadoDeCorreo(proveedorId, true, null);
        }

        public static ResultadoDeCorreo fallo(String error) {
            return new ResultadoDeCorreo(null, false, error);
        }
    }
}
