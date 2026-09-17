package com.regenta.comandas.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.regenta.comandas.BaseDeComandas;
import com.regenta.comandas.aplicacion.CatalogoDeMenu.ItemDeMenu;
import com.regenta.comandas.infra.CatalogoDeMenuStub;
import com.regenta.comun.errores.NoEncontradoException;
import com.regenta.comun.errores.ReglaDeNegocioException;

/**
 * HU-088. Enviar a cocina genera tickets por estación; cada estación ve solo lo
 * suyo, y marcar un ticket listo avisa al mesero. Con dos negocios cargados.
 */
class CocinaKdsTest extends BaseDeComandas {

    private static final Set<String> MESERO_PERMISOS =
            Set.of("COMANDAS_COMANDA_VER", "COMANDAS_COMANDA_CREAR", "COMANDAS_COMANDA_EDITAR");
    private static final Set<String> COCINERO_PERMISOS =
            Set.of("COMANDAS_COMANDA_VER", "COMANDAS_COMANDA_EDITAR");

    @Autowired
    private GestionDeComandas comandas;
    @Autowired
    private GestionDeCocina cocina;
    @Autowired
    private CatalogoDeMenuStub catalogo;

    private final UUID negocioA = UUID.randomUUID();
    private final UUID negocioB = UUID.randomUUID();
    private final UUID mesero = UUID.randomUUID();
    private final UUID cocinero = UUID.randomUUID();

    private final UUID parrilla = UUID.randomUUID();
    private final UUID bar = UUID.randomUUID();
    private final UUID churrasco = UUID.randomUUID();
    private final UUID limonada = UUID.randomUUID();

    @BeforeEach
    void catalogoDePrueba() {
        catalogo.reiniciar();
        catalogo.cargarItem(new ItemDeMenu(churrasco, "Churrasco 300 g", new BigDecimal("38000"),
                parrilla, "FUERTE", new BigDecimal("15000"), true));
        catalogo.cargarItem(new ItemDeMenu(limonada, "Limonada de coco", new BigDecimal("9000"), bar,
                "BEBIDA", new BigDecimal("2500"), true));
    }

    private UUID comandaConLineasEnviadas(UUID negocio) {
        UUID c = enContexto(negocio, mesero, MESERO_PERMISOS, () -> comandas.abrir(
                new SolicitudDeAperturaComanda(UUID.randomUUID(), UUID.randomUUID(), 2, null))).id();
        enContexto(negocio, mesero, MESERO_PERMISOS, () -> comandas.agregarLinea(c,
                new SolicitudDeLinea(churrasco, BigDecimal.ONE, null, null, null, null, null, null,
                        null)));
        enContexto(negocio, mesero, MESERO_PERMISOS, () -> comandas.agregarLinea(c,
                new SolicitudDeLinea(limonada, new BigDecimal("2"), null, null, null, null, null,
                        null, null)));
        enContexto(negocio, mesero, MESERO_PERMISOS, () -> comandas.enviarACocina(c));
        return c;
    }

    private List<TicketDetallado> ticketsDe(UUID negocio, UUID estacionId) {
        return enContexto(negocio, cocinero, COCINERO_PERMISOS, () -> cocina.ticketsDeEstacion(estacionId));
    }

    @Test
    @DisplayName("Criterio 1: al enviar la comanda se generan tickets separados por estación, con solo sus líneas")
    void ticketsPorEstacion() {
        comandaConLineasEnviadas(negocioA);

        List<TicketDetallado> deParrilla = ticketsDe(negocioA, parrilla);
        List<TicketDetallado> deBar = ticketsDe(negocioA, bar);

        assertThat(deParrilla).hasSize(1);
        assertThat(deParrilla.get(0).lineas()).extracting(TicketDetallado.LineaDeTicket::nombre)
                .containsExactly("Churrasco 300 g");
        assertThat(deBar).hasSize(1);
        assertThat(deBar.get(0).lineas()).extracting(TicketDetallado.LineaDeTicket::nombre)
                .containsExactly("Limonada de coco");
    }

    @Test
    @DisplayName("Criterio 2: la estación ve sus tickets NUEVO y EN_PREPARACION, pero no uno ya ENTREGADO")
    void columnasDeLaEstacion() {
        UUID c1 = comandaConLineasEnviadas(negocioA);
        comandaConLineasEnviadas(negocioA); // segunda comanda: otro ticket NUEVO en parrilla

        UUID ticket1 = ticketsDe(negocioA, parrilla).get(0).id();
        enContexto(negocioA, cocinero, COCINERO_PERMISOS, () -> cocina.avanzarTicket(ticket1)); // EN_PREPARACION

        List<TicketDetallado> vivos = ticketsDe(negocioA, parrilla);
        assertThat(vivos).extracting(TicketDetallado::estado)
                .containsExactlyInAnyOrder("NUEVO", "EN_PREPARACION");

        // Lo entrego del todo: LISTO -> ENTREGADO, y ya no aparece.
        enContexto(negocioA, cocinero, COCINERO_PERMISOS, () -> cocina.avanzarTicket(ticket1)); // LISTO
        enContexto(negocioA, cocinero, COCINERO_PERMISOS, () -> cocina.avanzarTicket(ticket1)); // ENTREGADO

        assertThat(ticketsDe(negocioA, parrilla)).extracting(TicketDetallado::id)
                .doesNotContain(ticket1);
        assertThat(c1).isNotNull();
    }

    @Test
    @DisplayName("Criterio 3: un ticket con más de 15 minutos se marca demorado")
    void ticketDemorado() {
        comandaConLineasEnviadas(negocioA);
        UUID ticket = ticketsDe(negocioA, parrilla).get(0).id();

        assertThat(ticketsDe(negocioA, parrilla).get(0).demorado()).isFalse();

        ejecutarComoElServicio(negocioA,
                "UPDATE tickets_cocina SET creado_en = now() - interval '20 minutes' WHERE id = '"
                        + ticket + "'");

        assertThat(ticketsDe(negocioA, parrilla).get(0).demorado()).isTrue();
    }

    @Test
    @DisplayName("Criterios 4 y 5: marcar el ticket listo publica linea_lista y la línea de la comanda pasa a LISTA")
    void marcarListoAvisaAlMesero() {
        UUID c = comandaConLineasEnviadas(negocioA);
        UUID ticket = ticketsDe(negocioA, parrilla).get(0).id();

        enContexto(negocioA, cocinero, COCINERO_PERMISOS, () -> cocina.avanzarTicket(ticket)); // EN_PREPARACION
        TicketDetallado listo = enContexto(negocioA, cocinero, COCINERO_PERMISOS,
                () -> cocina.avanzarTicket(ticket)); // LISTO

        assertThat(listo.estado()).isEqualTo("LISTO");
        assertThat(comoElServicio(negocioA, "select tipo_evento from outbox_eventos "
                + "where negocio_id = '" + negocioA + "' and tipo_evento = 'linea_lista'"))
                .containsExactly("linea_lista");

        ComandaDetallada comanda = enContexto(negocioA, mesero, MESERO_PERMISOS, () -> comandas.ver(c));
        assertThat(comanda.lineas()).filteredOn(l -> l.nombre().equals("Churrasco 300 g"))
                .extracting(ComandaDetallada.LineaDeComanda::estado).containsExactly("LISTA");
    }

    @Test
    @DisplayName("Un ticket ENTREGADO no avanza más")
    void noAvanzaMasAllaDeEntregado() {
        comandaConLineasEnviadas(negocioA);
        UUID ticket = ticketsDe(negocioA, parrilla).get(0).id();
        enContexto(negocioA, cocinero, COCINERO_PERMISOS, () -> cocina.avanzarTicket(ticket));
        enContexto(negocioA, cocinero, COCINERO_PERMISOS, () -> cocina.avanzarTicket(ticket));
        enContexto(negocioA, cocinero, COCINERO_PERMISOS, () -> cocina.avanzarTicket(ticket));

        assertThatThrownBy(() -> enContexto(negocioA, cocinero, COCINERO_PERMISOS,
                () -> cocina.avanzarTicket(ticket))).isInstanceOf(ReglaDeNegocioException.class);
    }

    @Test
    @DisplayName("El segundo negocio no ve los tickets del primero ni puede avanzarlos")
    void aislamiento() {
        comandaConLineasEnviadas(negocioA);
        UUID ticket = ticketsDe(negocioA, parrilla).get(0).id();

        assertThat(ticketsDe(negocioB, parrilla)).isEmpty();
        assertThatThrownBy(() -> enContexto(negocioB, cocinero, COCINERO_PERMISOS,
                () -> cocina.avanzarTicket(ticket))).isInstanceOf(NoEncontradoException.class);
    }
}
