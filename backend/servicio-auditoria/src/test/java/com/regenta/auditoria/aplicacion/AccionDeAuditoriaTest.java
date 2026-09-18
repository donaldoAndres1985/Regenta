package com.regenta.auditoria.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * HU-101. Traduce el nombre de un evento de dominio a una de las acciones que
 * acepta {@code eventos_auditoria.accion} (CHECK de la base): los eventos no
 * usan ese vocabulario, así que hace falta esta traducción.
 */
class AccionDeAuditoriaTest {

    @Test
    @DisplayName("Un evento de alta se traduce a CREAR")
    void alta() {
        assertThat(AccionDeAuditoria.desde("negocio_creado")).isEqualTo("CREAR");
        assertThat(AccionDeAuditoria.desde("cliente_creado")).isEqualTo("CREAR");
    }

    @Test
    @DisplayName("Un evento de anulación o cancelación se traduce a ANULAR")
    void anulacion() {
        assertThat(AccionDeAuditoria.desde("venta_anulada")).isEqualTo("ANULAR");
        assertThat(AccionDeAuditoria.desde("reserva_cancelada")).isEqualTo("ANULAR");
    }

    @Test
    @DisplayName("login_fallido se traduce literal, no como un intento de inicio de sesión exitoso")
    void loginFallido() {
        assertThat(AccionDeAuditoria.desde("login_fallido")).isEqualTo("LOGIN_FALLIDO");
    }

    @Test
    @DisplayName("Un evento sin patrón reconocido cae en ACTUALIZAR, no se descarta")
    void porDefecto() {
        assertThat(AccionDeAuditoria.desde("venta_completada")).isEqualTo("ACTUALIZAR");
        assertThat(AccionDeAuditoria.desde(null)).isEqualTo("ACTUALIZAR");
    }
}
