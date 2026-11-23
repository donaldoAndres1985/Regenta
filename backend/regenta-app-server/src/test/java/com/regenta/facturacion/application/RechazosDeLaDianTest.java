package com.regenta.facturacion.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.regenta.facturacion.application.RechazosDeLaDian.Rechazo;

/** HU-117 C4 · El rechazo dice qué campo lo causó y dónde se corrige. */
class RechazosDeLaDianTest {

    @Test
    void c4_unRechazoDelAdquirienteNombraElCampo() {
        Rechazo r = RechazosDeLaDian.explicar("99", "Validación contiene errores en campos mandatorios.",
                List.of("Regla: FAK21, Rechazo: ID de adquiriente no Informado"));

        assertThat(r.codigo()).isEqualTo("FAK21");
        assertThat(r.mensaje()).isEqualTo("Adquiriente · número de documento (datos del cliente): "
                + "ID de adquiriente no Informado (regla FAK21)");
    }

    @Test
    void c4_unRechazoDelEmisorDiceQueSeCorrigeEnLosDatosFiscales() {
        Rechazo r = RechazosDeLaDian.explicar("99", "con errores", List.of(
                "Regla: FAJ43b, Rechazo: Nombre informado No corresponde al registrado en el RUT con respecto al Nit "
                        + "suministrado.",
                "Regla: FAJ24, Rechazo: DV del NIT del emisor no informado"));

        assertThat(r.codigo()).isEqualTo("FAJ43b");
        assertThat(r.mensaje())
                .startsWith("Emisor · razón social (datos fiscales del negocio): Nombre informado No corresponde")
                .contains("Emisor · dígito de verificación (datos fiscales del negocio)");
    }

    @Test
    void lasNotificacionesNoSonElMotivo() {
        Rechazo r = RechazosDeLaDian.explicar("99", "con errores", List.of(
                "Regla: FAJ41, Notificación: El contenido de este elemento no corresponde al nombre y código valido.",
                "Regla: FAK26, Rechazo: Responsabilidad informada para receptor no válido según lista"));

        assertThat(r.codigo()).isEqualTo("FAK26");
        assertThat(r.mensaje()).startsWith("Adquiriente · responsabilidades fiscales").doesNotContain("FAJ41");
    }

    @Test
    void sinReglasQuedaElEstadoYLaDescripcion() {
        Rechazo r = RechazosDeLaDian.explicar("99", "Documento con errores", List.of("XML mal formado"));

        assertThat(r.codigo()).isEqualTo("99");
        assertThat(r.mensaje()).isEqualTo("Documento con errores: XML mal formado");
    }

    @Test
    void otrasReglasNombranLaFactura() {
        Rechazo r = RechazosDeLaDian.explicar("99", "x", List.of("Regla: FAD09, Rechazo: Fecha fuera del rango"));

        assertThat(r.mensaje()).isEqualTo("La factura: Fecha fuera del rango (regla FAD09)");
    }
}
