package com.regenta.auditoria.infrastructure;

import java.time.Duration;
import java.util.Set;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.regenta.auditoria.application.ArchivadorDeBitacora;
import com.regenta.shared.barridos.Barrido;

/** Los barridos de auditoría. */
@Configuration
public class BarridosDeAuditoria {

    /**
     * HU-131 · Cada negocio con bitácora pasa por el archivador, que decide con SU retención qué vence.
     * Una vez al día alcanza: la retención se mide en días.
     */
    @Bean
    public Barrido barridoDeBitacoraVencida(ArchivadorDeBitacora archivador) {
        return new Barrido("bitacora-vencida", """
                SELECT DISTINCT e.negocio_id
                  FROM auditoria.eventos_auditoria e
                  LEFT JOIN auditoria.politicas_retencion p ON p.negocio_id = e.negocio_id
                 WHERE e.ocurrido_en < now() - make_interval(days => COALESCE(p.dias_auditoria, 30))
                """, Set.of("AUDITORIA"), Set.of("AUDITORIA_BITACORA_VER"), Duration.ofDays(1),
                archivador::archivarVencidos);
    }
}
