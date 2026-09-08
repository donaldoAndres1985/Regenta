package com.regenta.alertas.aplicacion;

import java.util.Map;

/**
 * El envío de push (HU-094 criterio 1). La implementación real habla con
 * Firebase Cloud Messaging; hasta entonces, un stub configurable la reemplaza.
 */
public interface PasarelaDePush {

    ResultadoDePush enviar(String tokenFcm, String titulo, String cuerpo, Map<String, String> datos);

    /**
     * @param proveedorId  id del mensaje en FCM (o null si falló)
     * @param exito        se aceptó para entrega
     * @param tokenInvalido FCM dijo que el token no sirve → el dispositivo se desactiva (criterio 5)
     * @param error        detalle cuando no fue éxito
     */
    record ResultadoDePush(String proveedorId, boolean exito, boolean tokenInvalido, String error) {

        public static ResultadoDePush ok(String proveedorId) {
            return new ResultadoDePush(proveedorId, true, false, null);
        }

        public static ResultadoDePush fallo(String error) {
            return new ResultadoDePush(null, false, false, error);
        }

        public static ResultadoDePush porTokenInvalido() {
            return new ResultadoDePush(null, false, true, "token FCM no registrado");
        }
    }
}
