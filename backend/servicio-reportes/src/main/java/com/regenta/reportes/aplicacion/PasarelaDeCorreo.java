package com.regenta.reportes.aplicacion;

/** Por dónde sale el reporte programado (HU-100 criterio 2). */
public interface PasarelaDeCorreo {

    ResultadoDeCorreo enviar(String destino, String asunto, String cuerpo, ArchivoDeReporte adjunto);

    record ResultadoDeCorreo(boolean ok, String referencia, String error) {

        public static ResultadoDeCorreo ok(String referencia) {
            return new ResultadoDeCorreo(true, referencia, null);
        }

        public static ResultadoDeCorreo fallo(String error) {
            return new ResultadoDeCorreo(false, null, error);
        }
    }
}
