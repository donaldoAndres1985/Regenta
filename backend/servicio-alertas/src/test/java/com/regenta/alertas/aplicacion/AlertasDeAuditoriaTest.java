package com.regenta.alertas.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.regenta.alertas.BaseDeAlertas;
import com.regenta.alertas.infra.ConsumidorDeAuditoria;

/**
 * HU-103 criterio 5: un conflicto de sincronización sin resolver hace más de
 * un día genera una alerta ({@code SYNC_CONFLICTO}, ya en el catálogo desde
 * HU-092), en vez de quedar resuelto en silencio.
 */
class AlertasDeAuditoriaTest extends BaseDeAlertas {

    private static final Set<String> ADMIN = Set.of("ALERTAS_ALERTA_VER", "ALERTAS_ALERTA_EDITAR");

    @Autowired
    private GestionDeReglas reglas;
    @Autowired
    private ConsumidorDeAuditoria consumidor;
    @Autowired
    private ObjectMapper json;

    private final UUID negocioA = UUID.randomUUID();
    private final UUID admin = UUID.randomUUID();

    private void reglaDeConflicto(String nombre) {
        enContexto(negocioA, admin, ADMIN, () -> reglas.crear(new SolicitudDeRegla(
                "SYNC_CONFLICTO", nombre, null, Map.of(), "MEDIA", List.of("IN_APP"),
                List.of(), List.of(), "INMEDIATA", null, 0)));
    }

    private Message mensaje(String clave, Map<String, Object> payload) {
        try {
            byte[] cuerpo = json.writeValueAsBytes(payload);
            MessageProperties props = new MessageProperties();
            props.setMessageId(UUID.randomUUID().toString());
            props.setReceivedRoutingKey(clave);
            return MessageBuilder.withBody(cuerpo).andProperties(props).build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("Criterio 5: conflicto_sync_vencido genera la alerta SYNC_CONFLICTO")
    void conflictoVencidoGeneraAlerta() {
        reglaDeConflicto("Avisar conflictos");
        UUID conflictoId = UUID.randomUUID();
        UUID ventaId = UUID.randomUUID();

        consumidor.recibir(mensaje("conflicto_sync_vencido", Map.of(
                "negocio_id", negocioA.toString(),
                "conflicto_id", conflictoId.toString(),
                "entidad_tipo", "Venta",
                "entidad_id", ventaId.toString(),
                "tipo", "VERSION_DESACTUALIZADA")));

        assertThat(comoElServicio(negocioA,
                "select count(*) from alertas where tipo_codigo = 'SYNC_CONFLICTO' "
                        + "and entidad_id = '" + ventaId + "'"))
                .containsExactly("1");
        assertThat(comoElServicio(negocioA,
                "select ruta_app from alertas where entidad_id = '" + ventaId + "'"))
                .containsExactly("/auditoria/conflictos/" + conflictoId);
        assertThat(comoElServicio(negocioA,
                "select mensaje from alertas where entidad_id = '" + ventaId + "'"))
                .containsExactly("Una operación offline de Venta chocó al subir.");
    }

    @Test
    @DisplayName("El mismo conflicto_sync_vencido entregado dos veces no duplica la alerta")
    void eventoRepetidoNoDuplica() {
        reglaDeConflicto("Avisar conflictos");
        UUID conflictoId = UUID.randomUUID();
        UUID ventaId = UUID.randomUUID();
        String mensajeId = UUID.randomUUID().toString();
        Map<String, Object> payload = Map.of(
                "negocio_id", negocioA.toString(), "conflicto_id", conflictoId.toString(),
                "entidad_tipo", "Venta", "entidad_id", ventaId.toString(),
                "tipo", "VERSION_DESACTUALIZADA");

        Message primero = mensaje("conflicto_sync_vencido", payload);
        primero.getMessageProperties().setMessageId(mensajeId);
        Message segundo = mensaje("conflicto_sync_vencido", payload);
        segundo.getMessageProperties().setMessageId(mensajeId);
        consumidor.recibir(primero);
        consumidor.recibir(segundo);

        assertThat(comoElServicio(negocioA,
                "select count(*) from alertas where entidad_id = '" + ventaId + "'"))
                .containsExactly("1");
    }

    @Test
    @DisplayName("HU-043 criterio 4: una venta offline que chocó al subir avisa al vendedor que la hizo")
    void ventaOfflineEnConflictoAvisaAlVendedor() {
        reglaDeConflicto("Avisar conflictos");
        UUID ventaId = UUID.randomUUID();
        UUID vendedor = UUID.randomUUID();

        consumidor.recibir(mensaje("venta_offline_en_conflicto", Map.of(
                "negocio_id", negocioA.toString(),
                "venta_id", ventaId.toString(),
                "numero", "1042",
                "origen_offline_id", UUID.randomUUID().toString(),
                "usuario_id", vendedor.toString(),
                "motivo", "no hay stock suficiente")));

        assertThat(comoElServicio(negocioA,
                "select count(*) from alertas where tipo_codigo = 'SYNC_CONFLICTO' "
                        + "and entidad_id = '" + ventaId + "'"))
                .containsExactly("1");
        assertThat(comoElServicio(negocioA, "select ruta_app from alertas where entidad_id = '"
                + ventaId + "'")).containsExactly("/ventas/" + ventaId);
        assertThat(comoElServicio(negocioA,
                "select count(*) from entregas e join alertas a on a.id = e.alerta_id "
                        + "where a.entidad_id = '" + ventaId + "' and e.usuario_id = '" + vendedor + "'"))
                .as("la entrega va al vendedor que hizo la venta, no solo a quien diga la regla")
                .containsExactly("1");
    }

    @Test
    @DisplayName("Sin una regla activa para SYNC_CONFLICTO, el evento no genera alerta")
    void sinReglaNoGeneraAlerta() {
        UUID ventaId = UUID.randomUUID();

        consumidor.recibir(mensaje("conflicto_sync_vencido", Map.of(
                "negocio_id", negocioA.toString(), "conflicto_id", UUID.randomUUID().toString(),
                "entidad_tipo", "Venta", "entidad_id", ventaId.toString(),
                "tipo", "VERSION_DESACTUALIZADA")));

        assertThat(comoElServicio(negocioA,
                "select count(*) from alertas where entidad_id = '" + ventaId + "'"))
                .containsExactly("0");
    }
}
