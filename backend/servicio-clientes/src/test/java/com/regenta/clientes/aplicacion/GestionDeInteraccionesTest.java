package com.regenta.clientes.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.regenta.clientes.BaseDeClientes;
import com.regenta.comun.errores.NoEncontradoException;
import com.regenta.comun.errores.SinPermisoException;

/** HU-024. Historial de interacciones con el cliente. */
class GestionDeInteraccionesTest extends BaseDeClientes {

    private static final Set<String> DE_VENDEDOR = Set.of("CLIENTES_CLIENTE_VER",
            "CLIENTES_CLIENTE_CREAR", "CLIENTES_CLIENTE_EDITAR");
    private static final Set<String> SOLO_VER = Set.of("CLIENTES_CLIENTE_VER");

    @Autowired
    private GestionDeInteracciones interacciones;

    @Autowired
    private GestionDeClientes clientes;

    private final UUID negocioA = UUID.randomUUID();
    private final UUID negocioB = UUID.randomUUID();
    private final UUID vendedor = UUID.randomUUID();

    private UUID clienteEn(UUID negocio, String nombre) {
        return enContexto(negocio, vendedor, DE_VENDEDOR, () -> clientes.crear(new SolicitudDeCliente(
                "NATURAL", "CC", "" + nombre.hashCode(), null, nombre, "X", null, null, null, null,
                null, null))).id();
    }

    private static SolicitudDeInteraccion solicitud(String tipo, OffsetDateTime ocurrido,
            LocalDate seguimiento) {
        return new SolicitudDeInteraccion(tipo, "Cotizacion pendiente", "Pidio precio de 20 bultos",
                ocurrido, seguimiento);
    }

    @Test
    @DisplayName("Criterio 1: la interaccion queda con tipo, fecha, autor y detalle")
    void registraTipoFechaAutorDetalle() {
        UUID cliente = clienteEn(negocioA, "Ana");
        OffsetDateTime cuando = OffsetDateTime.parse("2026-09-01T15:00:00Z");

        InteraccionDelCliente creada = enContexto(negocioA, vendedor, DE_VENDEDOR,
                () -> interacciones.registrar(cliente, solicitud("LLAMADA", cuando, null)));

        assertThat(creada.tipo()).isEqualTo("LLAMADA");
        assertThat(creada.autorId()).isEqualTo(vendedor);
        assertThat(creada.ocurridoEn()).isEqualTo(cuando);
        assertThat(creada.detalle()).isEqualTo("Pidio precio de 20 bultos");

        assertThat(comoElServicio(negocioA, "select tipo || '|' || usuario_id || '|' || detalle"
                + " from interacciones where id = '" + creada.id() + "'"))
                .containsExactly("LLAMADA|" + vendedor + "|Pidio precio de 20 bultos");
    }

    @Test
    @DisplayName("Criterio 2: la ficha las muestra en orden cronologico inverso")
    void ordenCronologicoInverso() {
        UUID cliente = clienteEn(negocioA, "Ben");
        enContexto(negocioA, vendedor, DE_VENDEDOR, () -> {
            interacciones.registrar(cliente,
                    solicitud("NOTA", OffsetDateTime.parse("2026-09-01T09:00:00Z"), null));
            interacciones.registrar(cliente,
                    solicitud("VISITA", OffsetDateTime.parse("2026-09-05T09:00:00Z"), null));
            interacciones.registrar(cliente,
                    solicitud("WHATSAPP", OffsetDateTime.parse("2026-09-03T09:00:00Z"), null));
        });

        List<InteraccionDelCliente> historial = enContexto(negocioA, vendedor, SOLO_VER,
                () -> interacciones.listar(cliente));

        assertThat(historial).extracting(InteraccionDelCliente::tipo)
                .containsExactly("VISITA", "WHATSAPP", "NOTA");
    }

    @Test
    @DisplayName("Criterio 3: cuando llega la fecha de seguimiento se publica un recordatorio al responsable")
    void seguimientoVencidoPublicaRecordatorio() {
        UUID cliente = clienteEn(negocioA, "Cid");
        InteraccionDelCliente conSeguimiento = enContexto(negocioA, vendedor, DE_VENDEDOR,
                () -> interacciones.registrar(cliente, solicitud("LLAMADA",
                        OffsetDateTime.parse("2026-09-01T09:00:00Z"), LocalDate.parse("2026-09-10"))));

        int avisados = enContexto(negocioA, vendedor, DE_VENDEDOR,
                () -> interacciones.procesarSeguimientosPendientes(LocalDate.parse("2026-09-11")));
        assertThat(avisados).isEqualTo(1);

        assertThat(comoElServicio(negocioA, "select payload ->> 'responsable_id'"
                + " from outbox_eventos where tipo_evento = 'recordatorio_de_seguimiento'"
                + " and agregado_id = '" + conSeguimiento.id() + "'"))
                .containsExactly(vendedor.toString());
        assertThat(comoElServicio(negocioA, "select seguimiento_notificado_en is not null"
                + " from interacciones where id = '" + conSeguimiento.id() + "'"))
                .containsExactly("t");

        // Segunda corrida: ya avisado, no se repite.
        int repetido = enContexto(negocioA, vendedor, DE_VENDEDOR,
                () -> interacciones.procesarSeguimientosPendientes(LocalDate.parse("2026-09-12")));
        assertThat(repetido).isZero();
    }

    @Test
    @DisplayName("Un seguimiento cuya fecha aun no llega no dispara nada")
    void seguimientoFuturoNoAvisa() {
        UUID cliente = clienteEn(negocioA, "Dua");
        enContexto(negocioA, vendedor, DE_VENDEDOR, () -> interacciones.registrar(cliente,
                solicitud("NOTA", OffsetDateTime.parse("2026-09-01T09:00:00Z"),
                        LocalDate.parse("2026-12-01"))));

        int avisados = enContexto(negocioA, vendedor, DE_VENDEDOR,
                () -> interacciones.procesarSeguimientosPendientes(LocalDate.parse("2026-09-15")));

        assertThat(avisados).isZero();
    }

    @Test
    @DisplayName("Las interacciones de un negocio no se ven desde otro")
    void aisladoPorNegocio() {
        UUID clienteA = clienteEn(negocioA, "Eva");
        enContexto(negocioA, vendedor, DE_VENDEDOR, () -> interacciones.registrar(clienteA,
                solicitud("LLAMADA", OffsetDateTime.parse("2026-09-01T09:00:00Z"), null)));

        assertThat(comoElServicio(negocioB, "select count(*) from interacciones"))
                .containsExactly("0");
    }

    @Test
    @DisplayName("Registrar sobre un cliente que no existe en el negocio responde 404")
    void clienteInexistenteEs404() {
        UUID otro = UUID.randomUUID();
        assertThatThrownBy(() -> enContexto(negocioA, vendedor, DE_VENDEDOR,
                () -> interacciones.registrar(otro,
                        solicitud("NOTA", OffsetDateTime.now(), null))))
                .isInstanceOf(NoEncontradoException.class);
    }

    @Test
    @DisplayName("Sin el permiso de editar no se registra una interaccion")
    void sinPermisoNoRegistra() {
        UUID cliente = clienteEn(negocioA, "Fio");
        assertThatThrownBy(() -> enContexto(negocioA, vendedor, SOLO_VER,
                () -> interacciones.registrar(cliente,
                        solicitud("NOTA", OffsetDateTime.now(), null))))
                .isInstanceOf(SinPermisoException.class);
    }
}
