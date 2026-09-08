package com.regenta.recursos.aplicacion;

import java.util.UUID;

/**
 * Lo que servicio-recursos necesita saber de servicio-reservas para no borrar
 * un recurso con reservas futuras (HU-065 criterio 3). La implementación real
 * consulta por REST; hasta entonces, un stub la reemplaza.
 */
public interface ConsultaDeReservasDeRecurso {

    boolean tieneReservasFuturas(UUID negocioId, UUID recursoId);
}
