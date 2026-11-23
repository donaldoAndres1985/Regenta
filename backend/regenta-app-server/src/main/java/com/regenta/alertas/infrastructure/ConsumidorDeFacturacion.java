package com.regenta.alertas.infrastructure;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.regenta.alertas.application.VigilanciaDeFacturacion;
import com.regenta.shared.events.EventoGenerico;
import com.regenta.shared.events.InboxIdempotente;
import com.regenta.shared.tenancy.ContextoDeNegocio;
import com.regenta.shared.tenancy.DatosDelNegocio;

/** HU-116 C3 · {@code certificado_por_vencer} de Facturación → alerta {@code CERTIFICADO_POR_VENCER}. */
@Component
public class ConsumidorDeFacturacion {

    private final InboxIdempotente inbox;
    private final VigilanciaDeFacturacion vigilancia;
    private final ObjectMapper json;

    public ConsumidorDeFacturacion(InboxIdempotente inbox, VigilanciaDeFacturacion vigilancia, ObjectMapper json) {
        this.inbox = inbox;
        this.vigilancia = vigilancia;
        this.json = json;
    }

    @ApplicationModuleListener
    public void recibir(EventoGenerico evento) {
        if (evento == null || !"certificado_por_vencer".equals(evento.tipoEvento())) {
            return;
        }
        String cuerpo = serializar(evento.payload());
        Map<String, Object> datos = leer(cuerpo);
        Object negocio = datos.get("negocio_id") != null ? datos.get("negocio_id") : evento.negocioId();
        if (negocio == null) {
            return;
        }
        UUID negocioId = UUID.fromString(negocio.toString());
        ContextoDeNegocio.en(new DatosDelNegocio(negocioId, null, "", "", Set.of(), Set.of("ALERTAS"), Set.of(),
                Set.of()), () -> inbox.procesarUnaVez("alertas.ConsumidorDeFacturacion", evento.id(), negocioId,
                        evento.tipoEvento(), cuerpo, payload -> vigilancia.alCertificadoPorVencer(datos)));
    }

    private String serializar(Object payload) {
        if (payload == null) {
            return "{}";
        }
        if (payload instanceof String s) {
            return s;
        }
        try {
            return json.writeValueAsString(payload);
        } catch (Exception e) {
            return "{}";
        }
    }

    private Map<String, Object> leer(String cuerpo) {
        try {
            return json.readValue(cuerpo, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            throw new IllegalArgumentException("No se pudo leer el evento de facturación", e);
        }
    }
}
