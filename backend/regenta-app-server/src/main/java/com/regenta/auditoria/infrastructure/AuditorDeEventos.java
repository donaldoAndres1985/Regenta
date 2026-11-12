package com.regenta.auditoria.infrastructure;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.regenta.auditoria.application.AccionDeAuditoria;
import com.regenta.shared.events.CambiosDeAuditoria;
import com.regenta.shared.events.EventoGenerico;
import com.regenta.shared.events.InboxIdempotente;
import com.regenta.shared.tenancy.ContextoDeNegocio;
import com.regenta.shared.tenancy.DatosDelNegocio;

/**
 * HU-101 criterio 1: la bitácora de TODO lo que pasa en cualquier módulo.
 * En el monolito modular, escucha todos los eventos de dominio a través de
 * {@link EventoGenerico} vía Spring Modulith {@link ApplicationModuleListener}.
 */
@Component
public class AuditorDeEventos {

    private static final Set<String> TIPOS_DECLARADOS = Set.of("CREAR", "ACTUALIZAR", "ELIMINAR");

    private final InboxIdempotente inbox;
    private final EscritorDeAuditoria escritor;
    private final EscritorDeCambios cambios;
    private final ObjectMapper json;

    public AuditorDeEventos(InboxIdempotente inbox, EscritorDeAuditoria escritor, EscritorDeCambios cambios,
            ObjectMapper json) {
        this.inbox = inbox;
        this.escritor = escritor;
        this.cambios = cambios;
        this.json = json;
    }

    @ApplicationModuleListener
    public void recibir(EventoGenerico evento) {
        if (evento == null) {
            return;
        }
        UUID negocioId = evento.negocioId();
        if (negocioId == null) {
            return; // no es un evento de negocio: nada que auditar por negocio
        }
        UUID mensajeId = evento.id();
        String tipoEvento = evento.tipoEvento();
        String cuerpo = serializarPayload(evento.payload());

        Map<String, Object> headers = new HashMap<>();
        headers.put("negocio_id", negocioId.toString());
        headers.put("servicio_origen", "monolito");
        headers.put("agregado_tipo", evento.agregadoTipo());
        headers.put("agregado_id", evento.agregadoId() != null ? evento.agregadoId().toString() : null);
        headers.put("trace_id", evento.traceId());

        final UUID nId = negocioId;
        ContextoDeNegocio.en(contextoDe(nId), () -> inbox.procesarUnaVez("auditoria.AuditorDeEventos", mensajeId, nId,
                tipoEvento, cuerpo, payload -> procesar(mensajeId, nId, tipoEvento, headers, cuerpo)));
    }

    private void procesar(UUID mensajeId, UUID negocioId, String tipoEvento, Map<String, Object> headers,
            String cuerpo) {
        String servicio = texto(headers.get("servicio_origen"));
        String entidadTipo = texto(headers.get("agregado_tipo"));
        UUID entidadId = uuid(headers.get("agregado_id"));
        String traceId = texto(headers.get("trace_id"));
        Map<String, Object> payload = leer(cuerpo);
        UUID usuarioId = payload == null ? null : uuid(payload.get("usuario_id"));
        Map<?, ?> detalle = payload == null ? null
                : payload.remove(CambiosDeAuditoria.CLAVE) instanceof Map<?, ?> m ? m : null;
        String accion = accionDe(detalle, tipoEvento);
        String sinDetalle = payload == null ? cuerpo : aJson(payload);
        OffsetDateTime ahora = OffsetDateTime.now();

        // HU-130 criterio 2: se enmascara aquí también, por si el emisor se
        // olvidó. Criterio 4: sin detalle, el evento entra igual, con cambios NULL.
        escritor.insertar(mensajeId, negocioId, usuarioId,
                servicio == null || servicio.isBlank() ? "desconocido" : servicio,
                entidadTipo == null || entidadTipo.isBlank() ? "Desconocida" : entidadTipo, entidadId,
                accion, cambiosDe(detalle),
                payload == null ? cuerpo : aJson(CambiosDeAuditoria.enmascarar(payload)), traceId, ahora);

        // HU-104: el mismo evento alimenta el watermark de descarga incremental.
        if (entidadId != null) {
            cambios.insertar(negocioId, entidadTipo == null || entidadTipo.isBlank() ? "Desconocida"
                    : entidadTipo, entidadId, operacionDeCambio(accion), sinDetalle, ahora);
        }
    }

    private static String accionDe(Map<?, ?> detalle, String tipoEvento) {
        Object tipo = detalle == null ? null : detalle.get("tipo");
        if (tipo != null && TIPOS_DECLARADOS.contains(tipo.toString())) {
            return tipo.toString();
        }
        return AccionDeAuditoria.desde(tipoEvento);
    }

    private String cambiosDe(Map<?, ?> detalle) {
        if (detalle == null || !(detalle.get("cambios") instanceof Map<?, ?> cambiosDeclarados)) {
            return null;
        }
        Map<String, Object> limpio = new LinkedHashMap<>();
        cambiosDeclarados.forEach((campo, par) -> {
            String nombre = String.valueOf(campo);
            if (CambiosDeAuditoria.esSensible(nombre) && par instanceof Map<?, ?> antesYDespues) {
                Map<String, Object> oculto = new LinkedHashMap<>();
                antesYDespues.forEach((k, v) -> oculto.put(String.valueOf(k),
                        v == null ? null : CambiosDeAuditoria.OCULTO));
                limpio.put(nombre, oculto);
            } else {
                limpio.put(nombre, CambiosDeAuditoria.enmascarar(par));
            }
        });
        return aJson(limpio);
    }

    private String serializarPayload(Object payload) {
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
            return json.readValue(cuerpo, new TypeReference<LinkedHashMap<String, Object>>() {});
        } catch (Exception noEsUnObjeto) {
            return null;
        }
    }

    private String aJson(Object valor) {
        try {
            return json.writeValueAsString(valor);
        } catch (JsonProcessingException imposible) {
            throw new IllegalStateException(imposible);
        }
    }

    private static String operacionDeCambio(String accion) {
        return switch (accion) {
            case "CREAR" -> "CREAR";
            case "ELIMINAR" -> "ELIMINAR";
            default -> "ACTUALIZAR";
        };
    }

    private static String texto(Object valor) {
        return valor == null ? null : valor.toString();
    }

    private static UUID uuid(Object valor) {
        if (valor == null) {
            return null;
        }
        try {
            return UUID.fromString(valor.toString());
        } catch (IllegalArgumentException noEsUuid) {
            return null;
        }
    }

    private static DatosDelNegocio contextoDe(UUID negocioId) {
        return new DatosDelNegocio(negocioId, null, "", "VENTA_DIRECTA", Set.of(), Set.of("AUDITORIA"),
                Set.of(), Set.of());
    }
}
