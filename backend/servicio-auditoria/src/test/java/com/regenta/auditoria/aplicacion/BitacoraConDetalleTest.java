package com.regenta.auditoria.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
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
import com.regenta.auditoria.BaseDeAuditoria;
import com.regenta.auditoria.infra.AuditorDeEventos;
import com.regenta.comun.auditoria.CambiosDeAuditoria;

/**
 * HU-130. La bitácora dice qué cambió, no solo que cambió: los campos con su
 * valor anterior y el nuevo, cuando el servicio que escribe los adjunta.
 */
class BitacoraConDetalleTest extends BaseDeAuditoria {

    private static final Set<String> VER_AUDITORIA = Set.of("AUDITORIA_BITACORA_VER");

    @Autowired
    private ConsultaDeAuditoria consulta;
    @Autowired
    private AuditorDeEventos consumidor;
    @Autowired
    private ObjectMapper json;

    private final UUID negocioA = UUID.randomUUID();
    private final UUID negocioB = UUID.randomUUID();
    private final UUID producto = UUID.randomUUID();

    private void publicar(UUID negocio, String tipoEvento, Map<String, Object> payload) {
        try {
            MessageProperties props = new MessageProperties();
            props.setMessageId(UUID.randomUUID().toString());
            props.setReceivedRoutingKey(tipoEvento);
            props.setHeader("negocio_id", negocio.toString());
            props.setHeader("agregado_tipo", "Producto");
            props.setHeader("agregado_id", producto.toString());
            props.setHeader("servicio_origen", "servicio-inventario");
            Message mensaje = MessageBuilder.withBody(json.writeValueAsBytes(payload))
                    .andProperties(props).build();
            consumidor.recibir(mensaje);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static Map<String, Object> conDetalle(UUID negocio, Map<String, Object> bloque) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("negocio_id", negocio.toString());
        payload.put("producto_id", "x");
        payload.put(CambiosDeAuditoria.CLAVE, bloque);
        return payload;
    }

    private List<EventoDeAuditoria> historial(UUID negocio) {
        return enContexto(negocio, UUID.randomUUID(), VER_AUDITORIA,
                () -> consulta.porEntidad("Producto", producto));
    }

    @Test
    @DisplayName("Criterio 1: una entidad editada deja los campos que cambiaron con su valor anterior y el nuevo")
    void registraLosCamposQueCambiaron() {
        publicar(negocioA, "producto_actualizado", conDetalle(negocioA, CambiosDeAuditoria.actualizacion(
                Map.of("precio", 100, "nombre", "Martillo"), Map.of("precio", 120, "nombre", "Martillo"))));

        List<EventoDeAuditoria> eventos = historial(negocioA);

        assertThat(eventos).hasSize(1);
        assertThat(eventos.get(0).cambios()).containsOnlyKeys("precio");
        assertThat(eventos.get(0).cambios().get("precio"))
                .isEqualTo(Map.of("antes", 100, "despues", 120));
        assertThat(historial(negocioB)).as("otro negocio no ve el cambio").isEmpty();
    }

    @Test
    @DisplayName("Criterio 2: un campo sensible no queda en claro, ni en el detalle ni en el payload")
    void elCampoSensibleNoQuedaEnClaro() {
        // Un emisor que se olvidó de enmascarar: la bitácora lo hace igual.
        Map<String, Object> descuidado = Map.of("tipo", "ACTUALIZAR", "cambios",
                Map.of("pin_supervisor", Map.of("antes", "1234", "despues", "9876")));
        Map<String, Object> payload = conDetalle(negocioA, descuidado);
        payload.put("pin_supervisor", "9876");

        publicar(negocioA, "producto_actualizado", payload);

        String fila = comoElServicio(negocioA, "select coalesce(cambios::text,'') || datos_despues::text "
                + "from eventos_auditoria where entidad_id = '" + producto + "'").get(0);
        assertThat(fila).doesNotContain("1234", "9876").contains("pin_supervisor");
    }

    @Test
    @DisplayName("Criterio 3: crear y borrar se distinguen del cambio parcial")
    void crearYBorrarSeDistinguen() {
        // El nombre del evento no lo dice; el detalle sí.
        publicar(negocioA, "producto_sincronizado",
                conDetalle(negocioA, CambiosDeAuditoria.creacion(Map.of("precio", 100))));
        publicar(negocioA, "producto_sincronizado",
                conDetalle(negocioA, CambiosDeAuditoria.eliminacion(Map.of("precio", 100))));

        assertThat(historial(negocioA)).extracting(EventoDeAuditoria::accion)
                .containsExactlyInAnyOrder("CREAR", "ELIMINAR");
    }

    @Test
    @DisplayName("Criterio 4: un servicio sin instrumentar sigue quedando en la bitácora, sin detalle")
    void sinInstrumentarSeRegistraSinDetalle() {
        publicar(negocioA, "producto_actualizado", Map.of("negocio_id", negocioA.toString()));

        List<EventoDeAuditoria> eventos = historial(negocioA);

        assertThat(eventos).hasSize(1);
        assertThat(eventos.get(0).accion()).isEqualTo("ACTUALIZAR");
        assertThat(eventos.get(0).cambios()).isNull();
    }

    @Test
    @DisplayName("Criterio 5: la consulta por entidad sigue usando el índice que ya existe")
    void laConsultaPorEntidadUsaElIndice() {
        comoSuperusuario("""
                INSERT INTO auditoria.eventos_auditoria (id, negocio_id, servicio, entidad_tipo, entidad_id,
                    accion, cambios, ocurrido_en)
                SELECT gen_random_uuid(), ('00000000-0000-0000-0000-' || lpad((g % 40)::text, 12, '0'))::uuid,
                    'servicio-inventario', 'Producto', gen_random_uuid(), 'ACTUALIZAR',
                    '{"precio":{"antes":1,"despues":2}}'::jsonb, now() - (g || ' minutes')::interval
                FROM generate_series(1, 20000) g
                """);
        comoSuperusuario("ANALYZE auditoria.eventos_auditoria");

        String plan = String.join("\n", comoElServicio(negocioA, "EXPLAIN "
                + "SELECT id, cambios FROM eventos_auditoria WHERE negocio_id = '" + negocioA
                + "' AND entidad_tipo = 'Producto' AND entidad_id = '" + producto
                + "' ORDER BY ocurrido_en DESC"));

        assertThat(plan).contains("Index").doesNotContain("Seq Scan");
    }
}
