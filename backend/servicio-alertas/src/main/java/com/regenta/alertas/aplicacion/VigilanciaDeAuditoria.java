package com.regenta.alertas.aplicacion;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Traduce {@code conflicto_sync_vencido} (HU-103 criterio 5, publicado por
 * {@code servicio-auditoria} cuando un conflicto de sincronización lleva más
 * de un día sin resolver) a la alerta {@code SYNC_CONFLICTO}, ya en el
 * catálogo desde HU-092.
 */
@Service
public class VigilanciaDeAuditoria {

    private final MotorDeAlertas motor;

    public VigilanciaDeAuditoria(MotorDeAlertas motor) {
        this.motor = motor;
    }

    @Transactional
    public List<UUID> alConflictoSinResolver(Map<String, Object> datos) {
        UUID conflictoId = uuid(datos.get("conflicto_id"));
        String entidadTipo = texto(datos.get("entidad_tipo"), "un registro");
        UUID entidadId = uuid(datos.get("entidad_id"));
        Map<String, Object> campos = new LinkedHashMap<>();
        campos.put("entidad", entidadTipo);
        String ruta = conflictoId == null ? null : "/auditoria/conflictos/" + conflictoId;
        return motor.evaluar(new HechoDeAlerta("SYNC_CONFLICTO", entidadTipo, entidadId, ruta, null, campos));
    }

    private static UUID uuid(Object valor) {
        return valor == null ? null : UUID.fromString(valor.toString());
    }

    private static String texto(Object valor, String porDefecto) {
        return valor == null ? porDefecto : valor.toString();
    }
}
