package com.regenta.auditoria.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.regenta.auditoria.BaseDeAuditoria;

/** HU-104. */
class DescargaIncrementalTest extends BaseDeAuditoria {

    @Autowired
    private DescargaIncremental descarga;

    private final UUID negocio = UUID.randomUUID();

    private void registrarDispositivo(String identificador, String ultimoSyncEnExpr) {
        ejecutarComoElServicio(negocio, """
                insert into dispositivos (id, negocio_id, usuario_id, identificador, plataforma,
                    ultimo_sync_en, registrado_en)
                values ('%s', '%s', '%s', '%s', 'ANDROID', %s, now())
                """.formatted(UUID.randomUUID(), negocio, UUID.randomUUID(), identificador,
                ultimoSyncEnExpr));
    }

    private long insertarCambio(UUID entidadId) {
        ejecutarComoElServicio(negocio, """
                insert into cambios_servidor (negocio_id, entidad_tipo, entidad_id, operacion, version,
                    payload, ocurrido_en)
                values ('%s', 'Venta', '%s', 'ACTUALIZAR', 0, '{}', now())
                """.formatted(negocio, entidadId));
        return Long.parseLong(consultar("select max(id) from cambios_servidor "
                + "where negocio_id = '" + negocio + "' and entidad_id = '" + entidadId + "'").get(0));
    }

    @Test
    @DisplayName("Criterio 1: solo llegan los cambios posteriores al cursor")
    void soloLoPosteriorAlCursor() {
        registrarDispositivo("celular-de-ana", "now()");
        long primero = insertarCambio(UUID.randomUUID());
        insertarCambio(UUID.randomUUID());
        insertarCambio(UUID.randomUUID());

        ResultadoDeDescarga resultado =
                enContexto(negocio, null, Set.of(), () -> descarga.cambiosDesde("celular-de-ana", primero));

        assertThat(resultado.completaForzada()).isFalse();
        assertThat(resultado.cambios()).hasSize(2);
        assertThat(resultado.cambios()).allSatisfy(c -> assertThat(c.cursor()).isGreaterThan(primero));
    }

    @Test
    @DisplayName("Criterio 2: el cursor del dispositivo solo avanza al confirmar, no al pedir cambios")
    void elCursorAvanzaSoloAlConfirmar() {
        registrarDispositivo("celular-de-ana", "now()");
        long cambio = insertarCambio(UUID.randomUUID());

        enContexto(negocio, null, Set.of(), () -> descarga.cambiosDesde("celular-de-ana", 0L));
        assertThat(consultar("select cursor_sync from dispositivos where negocio_id = '" + negocio
                + "' and identificador = 'celular-de-ana'")).containsExactly((String) null);

        enContexto(negocio, null, Set.of(), () -> {
            descarga.confirmarDescarga("celular-de-ana", cambio);
            return null;
        });
        assertThat(consultar("select cursor_sync from dispositivos where negocio_id = '" + negocio
                + "' and identificador = 'celular-de-ana'")).containsExactly(Long.toString(cambio));
    }

    @Test
    @DisplayName("Criterio 3: una descarga interrumpida y repetida con el mismo cursor no pierde ni repite nada")
    void resumeSinPerderNiRepetir() {
        registrarDispositivo("celular-de-ana", "now()");
        long antes = insertarCambio(UUID.randomUUID());
        insertarCambio(UUID.randomUUID());
        insertarCambio(UUID.randomUUID());

        ResultadoDeDescarga primerIntento =
                enContexto(negocio, null, Set.of(), () -> descarga.cambiosDesde("celular-de-ana", antes));
        // Se "interrumpe": nunca se confirma, así que el cursor del dispositivo no avanzó.
        ResultadoDeDescarga reintento =
                enContexto(negocio, null, Set.of(), () -> descarga.cambiosDesde("celular-de-ana", antes));

        assertThat(reintento.cambios()).hasSize(primerIntento.cambios().size());
        assertThat(reintento.cambios()).extracting(CambioDeServidor::cursor)
                .containsExactlyElementsOf(primerIntento.cambios().stream().map(CambioDeServidor::cursor)
                        .toList());
    }

    @Test
    @DisplayName("Criterio 4: un dispositivo sin sincronizar hace más del período de retención fuerza descarga completa")
    void dispositivoViejoFuerzaDescargaCompleta() {
        registrarDispositivo("celular-viejo", "now() - interval '400 days'");
        long primero = insertarCambio(UUID.randomUUID());
        insertarCambio(UUID.randomUUID());

        // Aunque pida desde un cursor avanzado, se ignora y se le manda todo.
        ResultadoDeDescarga resultado = enContexto(negocio, null, Set.of(),
                () -> descarga.cambiosDesde("celular-viejo", primero));

        assertThat(resultado.completaForzada()).isTrue();
        assertThat(resultado.cambios()).hasSize(2);
    }

    @Test
    @DisplayName("Criterio 4: un dispositivo nunca visto también fuerza descarga completa")
    void dispositivoNuncaVistoFuerzaDescargaCompleta() {
        insertarCambio(UUID.randomUUID());

        ResultadoDeDescarga resultado = enContexto(negocio, null, Set.of(),
                () -> descarga.cambiosDesde("dispositivo-fantasma", 999L));

        assertThat(resultado.completaForzada()).isTrue();
        assertThat(resultado.cambios()).hasSize(1);
    }

    @Test
    @DisplayName("El segundo negocio no ve los cambios del primero")
    void aislamiento() {
        UUID negocioB = UUID.randomUUID();
        registrarDispositivo("celular-de-ana", "now()");
        insertarCambio(UUID.randomUUID());

        List<CambioDeServidor> cambios = enContexto(negocioB, null, Set.of(),
                () -> descarga.cambiosDesde("celular-de-ana", 0L).cambios());
        assertThat(cambios).isEmpty();
    }
}
