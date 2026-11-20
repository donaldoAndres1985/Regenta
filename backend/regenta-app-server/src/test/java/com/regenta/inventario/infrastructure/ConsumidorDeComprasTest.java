package com.regenta.inventario.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.regenta.inventario.application.EntradaDeCompras;
import com.regenta.inventario.application.EntradaDeRecepcion;
import com.regenta.shared.events.EventoGenerico;
import com.regenta.shared.events.InboxIdempotente;
import com.regenta.shared.tenancy.ContextoDeNegocio;

/** HU-165 · Inventario escucha {@code recepcion_registrada} (brecha B1 de Compras). */
class ConsumidorDeComprasTest {

    private final InboxIdempotente inbox = mock(InboxIdempotente.class);
    private final EntradaDeCompras entradas = mock(EntradaDeCompras.class);
    private final ConsumidorDeCompras consumidor =
            new ConsumidorDeCompras(inbox, entradas, new ObjectMapper());

    private final UUID negocioId = UUID.randomUUID();
    private final UUID usuarioId = UUID.randomUUID();
    private final UUID recepcionId = UUID.randomUUID();
    private final UUID bodegaId = UUID.randomUUID();
    private final UUID productoId = UUID.randomUUID();

    private Map<String, Object> payload() {
        Map<String, Object> linea = new LinkedHashMap<>();
        linea.put("producto_id", productoId.toString());
        linea.put("cantidad", "10.000000");
        linea.put("costo_unitario", "10500.0000");
        linea.put("codigo_lote", "L-1");
        linea.put("fecha_vencimiento", "2027-06-30");
        linea.put("registro_sanitario", "INVIMA-1");
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("negocio_id", negocioId.toString());
        datos.put("recepcion_id", recepcionId.toString());
        datos.put("bodega_id", bodegaId.toString());
        datos.put("numero", "REC-7");
        datos.put("usuario_id", usuarioId.toString());
        datos.put("lineas", List.of(linea));
        return datos;
    }

    private EventoGenerico evento(String tipo) {
        return new EventoGenerico(UUID.randomUUID(), negocioId, "Recepcion", recepcionId, tipo,
                payload(), null, OffsetDateTime.now());
    }

    /** El inbox de prueba ejecuta el efecto, como lo haría con un mensaje nuevo. */
    @SuppressWarnings("unchecked")
    private void inboxProcesa() {
        doAnswer(i -> {
            ((Consumer<String>) i.getArgument(5)).accept(i.getArgument(4));
            return true;
        }).when(inbox).procesarUnaVez(anyString(), any(), any(), anyString(), anyString(), any());
    }

    @Test
    @DisplayName("HU-165 C1 · Al llegar recepcion_registrada, Inventario da entrada a lo recibido")
    void daEntrada() {
        inboxProcesa();

        consumidor.recibir(evento("recepcion_registrada"));

        ArgumentCaptor<EntradaDeRecepcion> e = ArgumentCaptor.forClass(EntradaDeRecepcion.class);
        verify(entradas).registrar(e.capture());
        EntradaDeRecepcion entrada = e.getValue();
        assertThat(entrada.recepcionId()).isEqualTo(recepcionId);
        assertThat(entrada.numero()).isEqualTo("REC-7");
        assertThat(entrada.bodegaId()).isEqualTo(bodegaId);
        EntradaDeRecepcion.Linea l = entrada.lineas().get(0);
        assertThat(l.productoId()).isEqualTo(productoId);
        assertThat(l.cantidad()).isEqualByComparingTo("10");
        assertThat(l.costoUnitario()).isEqualByComparingTo("10500");
        assertThat(l.codigoLote()).isEqualTo("L-1");
        assertThat(l.fechaVencimiento()).isEqualTo(LocalDate.of(2027, 6, 30));
        assertThat(l.registroSanitario()).isEqualTo("INVIMA-1");
    }

    @Test
    @DisplayName("HU-165 C3 · La entrada corre como el negocio y el usuario que confirmó la recepción")
    void correComoElUsuario() {
        AtomicReference<UUID> negocio = new AtomicReference<>();
        AtomicReference<UUID> usuario = new AtomicReference<>();
        inboxProcesa();
        doAnswer(i -> {
            negocio.set(ContextoDeNegocio.actual().negocio());
            usuario.set(ContextoDeNegocio.actual().usuario());
            return null;
        }).when(entradas).registrar(any());

        consumidor.recibir(evento("recepcion_registrada"));

        assertThat(negocio.get()).isEqualTo(negocioId);
        assertThat(usuario.get()).isEqualTo(usuarioId);
    }

    @Test
    @DisplayName("HU-165 C4 · Pasa por el inbox con el id del mensaje: un duplicado no se procesa dos veces")
    void pasaPorElInbox() {
        EventoGenerico e = evento("recepcion_registrada");

        consumidor.recibir(e);

        verify(inbox).procesarUnaVez(eq("inventario.ConsumidorDeCompras"), eq(e.id()), eq(negocioId),
                eq("recepcion_registrada"), anyString(), any());
        verifyNoInteractions(entradas);   // el inbox de prueba no ejecutó el efecto: era repetido
    }

    @Test
    @DisplayName("HU-165 · Los demás eventos no le interesan")
    void ignoraOtrosEventos() {
        consumidor.recibir(evento("orden_compra_aprobada"));

        verifyNoInteractions(inbox, entradas);
    }
}
