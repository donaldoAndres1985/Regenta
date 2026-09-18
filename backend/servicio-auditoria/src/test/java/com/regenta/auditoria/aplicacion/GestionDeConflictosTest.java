package com.regenta.auditoria.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.regenta.auditoria.BaseDeAuditoria;
import com.regenta.comun.errores.ReglaDeNegocioException;

/** HU-103 criterios 2 y 3. */
class GestionDeConflictosTest extends BaseDeAuditoria {

    private static final Set<String> VER_AUDITORIA = Set.of("AUDITORIA_BITACORA_VER");

    @Autowired
    private GestionDeConflictos gestion;

    private final UUID negocio = UUID.randomUUID();
    private final UUID negocioB = UUID.randomUUID();
    private final UUID usuario = UUID.randomUUID();

    private UUID conflictoDePrueba(UUID negocio) {
        return conflictoDePrueba(negocio, "now()");
    }

    private UUID conflictoDePrueba(UUID negocio, String detectadoEnExpr) {
        UUID dispositivoId = UUID.randomUUID();
        UUID operacionId = UUID.randomUUID();
        UUID id = UUID.randomUUID();
        ejecutarComoElServicio(negocio, """
                insert into dispositivos (id, negocio_id, usuario_id, identificador, plataforma,
                    registrado_en)
                values ('%s', '%s', '%s', '%s', 'ANDROID', now())
                """.formatted(dispositivoId, negocio, UUID.randomUUID(), dispositivoId));
        ejecutarComoElServicio(negocio, """
                insert into operaciones_sync (id, negocio_id, dispositivo_id, usuario_id, idempotency_key,
                    secuencia_local, entidad_tipo, entidad_id, operacion, payload, creado_cliente_en)
                values ('%s', '%s', '%s', '%s', '%s', 1, 'Venta', '%s', 'CREAR', '{}', now())
                """.formatted(operacionId, negocio, dispositivoId, UUID.randomUUID(), operacionId,
                UUID.randomUUID()));
        ejecutarComoElServicio(negocio, """
                insert into conflictos_sync (id, negocio_id, operacion_id, entidad_tipo, entidad_id, tipo,
                    version_servidor, version_cliente, datos_servidor, datos_cliente, detectado_en)
                values ('%s', '%s', '%s', 'Venta', '%s', 'VERSION_DESACTUALIZADA', 5, 3,
                    '{"total":90000}', '{"total":85000}', %s)
                """.formatted(id, negocio, operacionId, UUID.randomUUID(), detectadoEnExpr));
        return id;
    }

    @Test
    @DisplayName("Criterio 2: los pendientes traen la versión del servidor y la del cliente lado a lado")
    void pendientesConAmbasVersiones() {
        UUID id = conflictoDePrueba(negocio);

        List<ConflictoDetalle> filas = enContexto(negocio, null, VER_AUDITORIA, () -> gestion.pendientes());

        assertThat(filas).hasSize(1);
        assertThat(filas.get(0).id()).isEqualTo(id);
        assertThat(filas.get(0).datosServidor()).contains("90000");
        assertThat(filas.get(0).datosCliente()).contains("85000");
    }

    @Test
    @DisplayName("Criterio 3: al resolver, queda quién resolvió y cómo")
    void resolverDejaConstancia() {
        UUID id = conflictoDePrueba(negocio);

        enContexto(negocio, usuario, VER_AUDITORIA, () -> {
            gestion.resolver(id, "cliente_gana");
            return null;
        });

        assertThat(consultar("select resolucion from conflictos_sync where id = '" + id + "'"))
                .containsExactly("CLIENTE_GANA");
        assertThat(consultar("select resuelto_por from conflictos_sync where id = '" + id + "'"))
                .containsExactly(usuario.toString());
        assertThat(consultar("select resuelto_en is not null from conflictos_sync where id = '" + id + "'"))
                .containsExactly("t");

        // Resuelto, ya no aparece entre los pendientes.
        List<ConflictoDetalle> pendientes =
                enContexto(negocio, null, VER_AUDITORIA, () -> gestion.pendientes());
        assertThat(pendientes).isEmpty();
    }

    @Test
    @DisplayName("No se puede resolver dos veces el mismo conflicto")
    void noSeResuelveDosVeces() {
        UUID id = conflictoDePrueba(negocio);
        enContexto(negocio, usuario, VER_AUDITORIA, () -> {
            gestion.resolver(id, "SERVIDOR_GANA");
            return null;
        });

        assertThatThrownBy(() -> enContexto(negocio, usuario, VER_AUDITORIA, () -> {
            gestion.resolver(id, "CLIENTE_GANA");
            return null;
        })).isInstanceOf(ReglaDeNegocioException.class);
    }

    @Test
    @DisplayName("Una resolución que no está en el catálogo se rechaza")
    void resolucionInvalida() {
        UUID id = conflictoDePrueba(negocio);

        assertThatThrownBy(() -> enContexto(negocio, usuario, VER_AUDITORIA, () -> {
            gestion.resolver(id, "LO_QUE_SEA");
            return null;
        })).isInstanceOf(ReglaDeNegocioException.class);
    }

    @Test
    @DisplayName("El segundo negocio no ve los conflictos del primero")
    void aislamiento() {
        conflictoDePrueba(negocio);

        List<ConflictoDetalle> filas =
                enContexto(negocioB, null, VER_AUDITORIA, () -> gestion.pendientes());
        assertThat(filas).isEmpty();
    }

    @Test
    @DisplayName("Criterio 5: un conflicto que lleva más del umbral sin resolver publica conflicto_sync_vencido")
    void barrerSinResolverPublicaLosVencidos() {
        UUID viejo = conflictoDePrueba(negocio, "now() - interval '2 days'");
        conflictoDePrueba(negocio, "now()"); // recién detectado: no debe alertar todavía

        int publicados = enContexto(negocio, usuario, VER_AUDITORIA, () -> gestion.barrerSinResolver());

        assertThat(publicados).isEqualTo(1);
        assertThat(consultar("select tipo_evento from outbox_eventos where negocio_id = '" + negocio
                + "' and tipo_evento = 'conflicto_sync_vencido'")).hasSize(1);
        assertThat(consultar("select payload from outbox_eventos where negocio_id = '" + negocio
                + "' and tipo_evento = 'conflicto_sync_vencido'").get(0)).contains(viejo.toString());
    }

    @Test
    @DisplayName("Criterio 5: un conflicto ya resuelto no se vuelve a barrer aunque sea viejo")
    void barrerSinResolverIgnoraLosResueltos() {
        UUID viejo = conflictoDePrueba(negocio, "now() - interval '2 days'");
        enContexto(negocio, usuario, VER_AUDITORIA, () -> {
            gestion.resolver(viejo, "SERVIDOR_GANA");
            return null;
        });

        int publicados = enContexto(negocio, usuario, VER_AUDITORIA, () -> gestion.barrerSinResolver());

        assertThat(publicados).isZero();
    }
}
