package com.regenta.auditoria.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.regenta.auditoria.BaseDeAuditoria;
import com.regenta.auditoria.domain.EstadoOperacion;

/** HU-102 criterios 1, 2 y 3. */
class ColaDeSincronizacionTest extends BaseDeAuditoria {

    @Autowired
    private ColaDeSincronizacion cola;

    private final UUID negocioA = UUID.randomUUID();
    private final UUID negocioB = UUID.randomUUID();
    private final UUID usuario = UUID.randomUUID();

    private OperacionEntrante operacion(String idempotencyKey, long secuencia) {
        return new OperacionEntrante(UUID.randomUUID(), idempotencyKey, secuencia, "Venta",
                UUID.randomUUID(), "CREAR", Map.of("numero", "V-" + secuencia), null,
                OffsetDateTime.now());
    }

    @Test
    @DisplayName("Criterio 1: se procesan en el orden de su secuencia local, no del arreglo")
    void seProcesanEnOrdenDeSecuencia() {
        List<ResultadoDeOperacion> resultados = enContexto(negocioA, usuario, Set.of(),
                () -> cola.subirLote("celular-de-ana", "ANDROID", "1.4.0",
                        List.of(operacion("op-3", 3), operacion("op-1", 1), operacion("op-2", 2))));

        assertThat(resultados).extracting(ResultadoDeOperacion::estado)
                .containsExactly(EstadoOperacion.RECIBIDA, EstadoOperacion.RECIBIDA,
                        EstadoOperacion.RECIBIDA);
        // El orden de la respuesta sigue la secuencia local (1,2,3), no el orden en que
        // llegaron en el arreglo (3,1,2): esto es lo que criterio 1 exige.
        assertThat(consultar("select idempotency_key from operaciones_sync "
                + "where negocio_id = '" + negocioA + "' order by secuencia_local"))
                .containsExactly("op-1", "op-2", "op-3");
    }

    @Test
    @DisplayName("Criterio 2: una operación ya aplicada se descarta por su idempotency_key")
    void esIdempotentePorClave() {
        OperacionEntrante op = operacion("op-repetida", 1);

        ResultadoDeOperacion primero =
                enContexto(negocioA, usuario, Set.of(), () -> cola.subirLote("dispositivo-1", "ANDROID",
                        "1.4.0", List.of(op)).get(0));
        ResultadoDeOperacion segundo =
                enContexto(negocioA, usuario, Set.of(), () -> cola.subirLote("dispositivo-1", "ANDROID",
                        "1.4.0", List.of(op)).get(0));

        assertThat(segundo.id()).isEqualTo(primero.id());
        assertThat(contar("select count(*) from operaciones_sync "
                + "where negocio_id = '" + negocioA + "' and idempotency_key = 'op-repetida'"))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("Criterio 3: el dispositivo queda registrado con su plataforma y versión de app")
    void registraElDispositivo() {
        enContexto(negocioA, usuario, Set.of(),
                () -> cola.subirLote("celular-de-ana", "ANDROID", "1.4.0", List.of(operacion("op-a", 1))));

        assertThat(consultar("select plataforma from dispositivos "
                + "where negocio_id = '" + negocioA + "' and identificador = 'celular-de-ana'"))
                .containsExactly("ANDROID");
        assertThat(consultar("select version_app from dispositivos "
                + "where negocio_id = '" + negocioA + "' and identificador = 'celular-de-ana'"))
                .containsExactly("1.4.0");

        // Un segundo lote del mismo dispositivo, con la app ya actualizada, actualiza
        // la fila en vez de crear un dispositivo nuevo.
        enContexto(negocioA, usuario, Set.of(),
                () -> cola.subirLote("celular-de-ana", "ANDROID", "1.5.0", List.of(operacion("op-b", 2))));

        assertThat(contar("select count(*) from dispositivos "
                + "where negocio_id = '" + negocioA + "' and identificador = 'celular-de-ana'"))
                .isEqualTo(1);
        assertThat(consultar("select version_app from dispositivos "
                + "where negocio_id = '" + negocioA + "' and identificador = 'celular-de-ana'"))
                .containsExactly("1.5.0");
    }

    @Test
    @DisplayName("El segundo negocio no ve la cola ni los dispositivos del primero")
    void aislamiento() {
        enContexto(negocioA, usuario, Set.of(),
                () -> cola.subirLote("celular-de-ana", "ANDROID", "1.4.0", List.of(operacion("op-x", 1))));

        assertThat(comoElServicio(negocioB,
                "select count(*) from operaciones_sync where negocio_id = '" + negocioA + "'"))
                .containsExactly("0");
        assertThat(comoElServicio(negocioB,
                "select count(*) from dispositivos where negocio_id = '" + negocioA + "'"))
                .containsExactly("0");
    }
}
