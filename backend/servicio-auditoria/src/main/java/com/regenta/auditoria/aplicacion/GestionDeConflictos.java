package com.regenta.auditoria.aplicacion;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.auditoria.domain.ConflictoSync;
import com.regenta.auditoria.infra.ConflictoSyncRepositorio;
import com.regenta.comun.errores.NoEncontradoException;
import com.regenta.comun.errores.ReglaDeNegocioException;
import com.regenta.comun.eventos.RegistroDeEventos;
import com.regenta.comun.negocio.ContextoDeNegocio;
import com.regenta.comun.negocio.RequierePermiso;

/** Consultar y resolver conflictos de sincronización. HU-103 criterios 2, 3 y 5. */
@Service
public class GestionDeConflictos {

    private static final Set<String> RESOLUCIONES_VALIDAS =
            Set.of("SERVIDOR_GANA", "CLIENTE_GANA", "FUSION", "MANUAL", "DESCARTADO");

    /**
     * Cuánto puede llevar un conflicto sin resolver antes de escalar a alerta
     * (criterio 5). La historia no fija un número: se usa el mismo horizonte
     * que {@code silenciar_horas} por defecto en {@code reglas_alerta} —un día
     * es lo que el resto del sistema de alertas ya trata como "demasiado
     * tiempo sin que nadie lo mire".
     */
    private static final Duration UMBRAL_SIN_RESOLVER = Duration.ofHours(24);

    private final ConflictoSyncRepositorio conflictos;
    private final RegistroDeEventos eventos;

    public GestionDeConflictos(ConflictoSyncRepositorio conflictos, RegistroDeEventos eventos) {
        this.conflictos = conflictos;
        this.eventos = eventos;
    }

    /** Criterio 2: la versión del servidor y la del cliente lado a lado. */
    @Transactional(readOnly = true)
    @RequierePermiso("AUDITORIA_BITACORA_VER")
    public List<ConflictoDetalle> pendientes() {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        return conflictos.findByNegocioIdAndResolucionIsNullOrderByDetectadoEnDesc(negocioId).stream()
                .map(this::detalle)
                .toList();
    }

    /** Criterio 3: queda quién resolvió y cómo. */
    @Transactional
    @RequierePermiso("AUDITORIA_BITACORA_VER")
    public void resolver(UUID conflictoId, String resolucion) {
        String elegida = resolucion == null ? "" : resolucion.trim().toUpperCase(Locale.ROOT);
        if (!RESOLUCIONES_VALIDAS.contains(elegida)) {
            throw new ReglaDeNegocioException("Resolución no válida: " + resolucion);
        }
        ConflictoSync conflicto = conflictos.findById(conflictoId)
                .orElseThrow(() -> new NoEncontradoException("Ese conflicto no existe"));
        if (conflicto.estaResuelto()) {
            throw new ReglaDeNegocioException("Ese conflicto ya se había resuelto");
        }
        conflicto.resolver(elegida, ContextoDeNegocio.usuarioActual(), OffsetDateTime.now());
        conflictos.save(conflicto);
    }

    /**
     * Criterio 5: publica {@code conflicto_sync_vencido} por cada conflicto sin
     * resolver desde antes de {@link #UMBRAL_SIN_RESOLVER}. El barrido
     * verdaderamente multi-tenant ({@code @Scheduled} sobre todos los negocios)
     * necesita el rol privilegiado que queda diferido —misma nota que
     * {@code GestionDeVigilancia.barrerVencimientos} en servicio-alertas—; esto
     * es el disparo manual por negocio.
     */
    @Transactional
    @RequierePermiso("AUDITORIA_BITACORA_VER")
    public int barrerSinResolver() {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        OffsetDateTime limite = OffsetDateTime.now().minus(UMBRAL_SIN_RESOLVER);
        List<ConflictoSync> vencidos =
                conflictos.findByNegocioIdAndResolucionIsNullAndDetectadoEnBefore(negocioId, limite);
        for (ConflictoSync c : vencidos) {
            eventos.registrar(negocioId, c.getEntidadTipo(), c.getEntidadId(), "conflicto_sync_vencido",
                    payloadVencido(negocioId, c));
        }
        return vencidos.size();
    }

    private Map<String, Object> payloadVencido(UUID negocioId, ConflictoSync c) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("negocio_id", negocioId.toString());
        datos.put("conflicto_id", c.getId().toString());
        datos.put("entidad_tipo", c.getEntidadTipo());
        datos.put("entidad_id", c.getEntidadId() == null ? null : c.getEntidadId().toString());
        datos.put("tipo", c.getTipo());
        datos.put("detectado_en", c.getDetectadoEn().toString());
        return datos;
    }

    private ConflictoDetalle detalle(ConflictoSync c) {
        return new ConflictoDetalle(c.getId(), c.getEntidadTipo(), c.getEntidadId(), c.getTipo(),
                c.getDatosServidor(), c.getDatosCliente(), c.getResolucion(), c.getDetectadoEn());
    }
}
