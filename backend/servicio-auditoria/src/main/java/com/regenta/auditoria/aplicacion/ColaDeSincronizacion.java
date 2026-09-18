package com.regenta.auditoria.aplicacion;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.regenta.auditoria.domain.Dispositivo;
import com.regenta.auditoria.domain.EstadoOperacion;
import com.regenta.auditoria.domain.OperacionSync;
import com.regenta.auditoria.infra.DispositivoRepositorio;
import com.regenta.auditoria.infra.OperacionSyncRepositorio;
import com.regenta.comun.eventos.RegistroDeEventos;
import com.regenta.comun.negocio.ContextoDeNegocio;

/**
 * La cola de subida de operaciones offline (HU-102). Servicio-auditoria no
 * conoce las reglas de negocio de una venta o un cliente —esas son de cada
 * servicio dueño—, así que "aplicar" una operación aquí es publicarla
 * ({@code operacion_sync_recibida}) para que su servicio dueño la procese con
 * sus propias reglas y avise el resultado por {@code operacion_sync_resultado}
 * ({@link com.regenta.auditoria.infra.ConsumidorDeResultadoDeOperacion}).
 */
@Service
public class ColaDeSincronizacion {

    private final DispositivoRepositorio dispositivos;
    private final OperacionSyncRepositorio operaciones;
    private final RegistroDeEventos eventos;
    private final ObjectMapper json;

    public ColaDeSincronizacion(DispositivoRepositorio dispositivos, OperacionSyncRepositorio operaciones,
            RegistroDeEventos eventos, ObjectMapper json) {
        this.dispositivos = dispositivos;
        this.operaciones = operaciones;
        this.eventos = eventos;
        this.json = json;
    }

    /**
     * Criterio 1: se procesan en el orden de su secuencia local, sin importar
     * en qué orden vinieron en el arreglo. Criterio 2: una ya aplicada (por su
     * idempotency_key) se descarta y devuelve el resultado anterior, no se
     * reprocesa.
     */
    @Transactional
    public List<ResultadoDeOperacion> subirLote(String identificadorDispositivo, String plataforma,
            String versionApp, List<OperacionEntrante> lote) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        UUID usuarioId = ContextoDeNegocio.usuarioActual();
        OffsetDateTime ahora = OffsetDateTime.now();

        UUID dispositivoId = registrarDispositivo(negocioId, usuarioId, identificadorDispositivo, plataforma,
                versionApp, ahora);

        List<OperacionEntrante> ordenadas = lote.stream()
                .sorted(Comparator.comparingLong(OperacionEntrante::secuenciaLocal))
                .toList();

        List<ResultadoDeOperacion> resultados = new ArrayList<>();
        for (OperacionEntrante entrante : ordenadas) {
            resultados.add(procesarUna(negocioId, dispositivoId, usuarioId, entrante, ahora));
        }
        return resultados;
    }

    private UUID registrarDispositivo(UUID negocioId, UUID usuarioId, String identificador, String plataforma,
            String versionApp, OffsetDateTime ahora) {
        Dispositivo dispositivo = dispositivos.findByNegocioIdAndIdentificador(negocioId, identificador)
                .map(d -> {
                    d.sincronizo(plataforma, versionApp, ahora);
                    return d;
                })
                .orElseGet(() -> Dispositivo.nuevo(negocioId, usuarioId, identificador, null, plataforma,
                        versionApp, ahora));
        return dispositivos.save(dispositivo).getId();
    }

    private ResultadoDeOperacion procesarUna(UUID negocioId, UUID dispositivoId, UUID usuarioId,
            OperacionEntrante entrante, OffsetDateTime ahora) {
        var existente = operaciones.findByNegocioIdAndIdempotencyKey(negocioId, entrante.idempotencyKey());
        if (existente.isPresent()) {
            OperacionSync o = existente.get();
            return new ResultadoDeOperacion(o.getId(), o.getEstado(), o.getError());
        }

        OperacionSync nueva = OperacionSync.recibida(entrante.id(), negocioId, dispositivoId, usuarioId,
                entrante.idempotencyKey(), entrante.secuenciaLocal(), entrante.entidadTipo(),
                entrante.entidadId(), entrante.operacion(), aJson(entrante.payload()), entrante.versionBase(),
                entrante.creadoClienteEn(), ahora);
        operaciones.save(nueva);

        eventos.registrar(negocioId, entrante.entidadTipo(), entrante.entidadId(), "operacion_sync_recibida",
                entrante.payload());

        return new ResultadoDeOperacion(nueva.getId(), EstadoOperacion.RECIBIDA, null);
    }

    private String aJson(Object payload) {
        try {
            return json.writeValueAsString(payload);
        } catch (Exception e) {
            throw new IllegalArgumentException("Payload de operación de sincronización inválido", e);
        }
    }
}
