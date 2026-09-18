package com.regenta.auditoria.aplicacion;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.auditoria.domain.ConflictoSync;
import com.regenta.auditoria.infra.ConflictoSyncRepositorio;
import com.regenta.comun.errores.NoEncontradoException;
import com.regenta.comun.errores.ReglaDeNegocioException;
import com.regenta.comun.negocio.ContextoDeNegocio;
import com.regenta.comun.negocio.RequierePermiso;

/** Consultar y resolver conflictos de sincronización. HU-103 criterios 2 y 3. */
@Service
public class GestionDeConflictos {

    private static final Set<String> RESOLUCIONES_VALIDAS =
            Set.of("SERVIDOR_GANA", "CLIENTE_GANA", "FUSION", "MANUAL", "DESCARTADO");

    private final ConflictoSyncRepositorio conflictos;

    public GestionDeConflictos(ConflictoSyncRepositorio conflictos) {
        this.conflictos = conflictos;
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

    private ConflictoDetalle detalle(ConflictoSync c) {
        return new ConflictoDetalle(c.getId(), c.getEntidadTipo(), c.getEntidadId(), c.getTipo(),
                c.getDatosServidor(), c.getDatosCliente(), c.getResolucion(), c.getDetectadoEn());
    }
}
