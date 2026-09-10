package com.regenta.comandas.aplicacion;

/** Cambiar el curso y/o la secuencia de envío de una línea aún pendiente (HU-086). */
public record SolicitudDeAjusteDeLinea(String curso, Integer secuenciaEnvio) {
}
