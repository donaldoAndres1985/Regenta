package com.regenta.alertas.aplicacion;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Las dos formas en que una operación hecha sin señal termina mal, las dos a
 * la alerta {@code SYNC_CONFLICTO} del catálogo (HU-092):
 *
 * <ul>
 *   <li>{@code conflicto_sync_vencido}: un conflicto de sincronización lleva
 *       más de un día sin que nadie lo resuelva (HU-103 criterio 5).</li>
 *   <li>{@code venta_offline_en_conflicto}: una venta registrada sin señal
 *       subió, pero para entonces ya no había stock (HU-043 criterio 4).</li>
 * </ul>
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

    /**
     * HU-043 criterio 4. Avisa además al vendedor que hizo la venta: es quien
     * tiene que decidir qué hacer con ella, y nadie estaba mirando la pantalla
     * cuando el stock se acabó.
     */
    @Transactional
    public List<UUID> alVentaOfflineEnConflicto(Map<String, Object> datos) {
        UUID ventaId = uuid(datos.get("venta_id"));
        UUID vendedor = uuid(datos.get("usuario_id"));
        Map<String, Object> campos = new LinkedHashMap<>();
        campos.put("entidad", "la venta " + texto(datos.get("numero"), "sin número"));
        campos.put("motivo", texto(datos.get("motivo"), "chocó al subir"));
        String ruta = ventaId == null ? null : "/ventas/" + ventaId;
        return motor.evaluar(new HechoDeAlerta("SYNC_CONFLICTO", "Venta", ventaId, ruta, null, campos,
                vendedor == null ? Set.of() : Set.of(vendedor)));
    }

    private static UUID uuid(Object valor) {
        return valor == null ? null : UUID.fromString(valor.toString());
    }

    private static String texto(Object valor, String porDefecto) {
        return valor == null ? porDefecto : valor.toString();
    }
}
