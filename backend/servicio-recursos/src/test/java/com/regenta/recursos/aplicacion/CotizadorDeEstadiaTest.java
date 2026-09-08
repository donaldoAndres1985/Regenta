package com.regenta.recursos.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.regenta.comun.errores.NoEncontradoException;
import com.regenta.recursos.BaseDeRecursos;

/**
 * HU-066. Cotiza una estancia noche por noche: entre las tarifas que aplican a
 * cada noche gana la de mayor prioridad, y una estancia larga puede cruzar
 * varias. Corre con dos negocios cargados: el segundo nunca ve las tarifas ni
 * los recursos del primero.
 */
class CotizadorDeEstadiaTest extends BaseDeRecursos {

    private static final Set<String> ADMIN = Set.of("RECURSOS_RECURSO_VER",
            "RECURSOS_RECURSO_CREAR", "RECURSOS_RECURSO_EDITAR");

    @Autowired
    private CotizadorDeEstadia cotizador;
    @Autowired
    private GestionDeTarifas tarifas;
    @Autowired
    private GestionDeTiposDeRecurso tipos;
    @Autowired
    private GestionDeRecursos recursos;

    private final UUID negocioA = UUID.randomUUID();
    private final UUID negocioB = UUID.randomUUID();
    private final UUID admin = UUID.randomUUID();

    private UUID tipo(UUID negocio, String nombre) {
        return enContexto(negocio, admin, ADMIN, () -> tipos.crear(new SolicitudDeTipoDeRecurso(
                nombre, null, "NOCHE", 1440, 1440, 2, false, 0, 0))).id();
    }

    private UUID recurso(UUID negocio, UUID tipo, String codigo) {
        return enContexto(negocio, admin, ADMIN, () -> recursos.crear(new SolicitudDeRecurso(
                tipo, codigo, "Recurso " + codigo, null, null, 4, "1", "Norte", Map.of(), null)))
                .id();
    }

    private UUID tarifaDeTipo(UUID negocio, UUID tipo, String nombre, String precioBase,
            String precioAdicional, LocalDate desde, LocalDate hasta, List<Integer> dias,
            int estanciaMinima, int prioridad) {
        return enContexto(negocio, admin, ADMIN, () -> tarifas.crear(new SolicitudDeTarifa(
                tipo, null, nombre, "NOCHE", new BigDecimal(precioBase), new BigDecimal(precioAdicional),
                desde, hasta, dias, null, null, estanciaMinima, prioridad))).id();
    }

    private CotizacionDeEstadia cotizar(UUID negocio, UUID recurso, LocalDate entrada, int noches,
            int personas) {
        return enContexto(negocio, admin, ADMIN, () -> cotizador.cotizar(
                new SolicitudDeCotizacion(recurso, entrada, noches, personas)));
    }

    @Test
    @DisplayName("Criterio 1: entre varias tarifas que aplican a la misma noche gana la de mayor prioridad")
    void ganaLaDeMayorPrioridad() {
        UUID t = tipo(negocioA, "Habitación");
        UUID r = recurso(negocioA, t, "101");
        UUID base = tarifaDeTipo(negocioA, t, "Base", "100000", "0", null, null, null, 1, 0);
        UUID especial = tarifaDeTipo(negocioA, t, "Puente", "180000", "0", null, null, null, 1, 5);

        CotizacionDeEstadia cot = cotizar(negocioA, r, LocalDate.of(2026, 5, 16), 1, 1);

        assertThat(cot.total()).isEqualByComparingTo("180000");
        assertThat(cot.completa()).isTrue();
        assertThat(cot.noches()).hasSize(1);
        assertThat(cot.noches().get(0).tarifaId()).isEqualTo(especial);
        assertThat(cot.noches().get(0).tarifaId()).isNotEqualTo(base);
    }

    @Test
    @DisplayName("Criterio 4: una estancia de tres noches cobra cada noche a su tarifa al cruzar temporada")
    void tresNochesCadaUnaASuTarifa() {
        UUID t = tipo(negocioA, "Suite");
        UUID r = recurso(negocioA, t, "S1");
        UUID base = tarifaDeTipo(negocioA, t, "Base", "100000", "0", null, null, null, 1, 0);
        UUID alta = tarifaDeTipo(negocioA, t, "Temporada alta", "250000", "0",
                LocalDate.of(2026, 12, 24), LocalDate.of(2026, 12, 26), null, 1, 10);

        CotizacionDeEstadia cot = cotizar(negocioA, r, LocalDate.of(2026, 12, 23), 3, 1);

        assertThat(cot.noches()).extracting(n -> n.tarifaId())
                .containsExactly(base, alta, alta);
        assertThat(cot.noches()).extracting(n -> n.precio())
                .containsExactly(new BigDecimal("100000.0000"), new BigDecimal("250000.0000"),
                        new BigDecimal("250000.0000"));
        assertThat(cot.total()).isEqualByComparingTo("600000");
        assertThat(cot.completa()).isTrue();
    }

    @Test
    @DisplayName("Criterio 3: al cotizar, una tarifa de fin de semana no cobra la noche de un jueves")
    void tarifaDeFinDeSemanaSoloEnSusDias() {
        UUID t = tipo(negocioA, "Cabaña");
        UUID r = recurso(negocioA, t, "CAB-1");
        UUID base = tarifaDeTipo(negocioA, t, "Base", "100000", "0", null, null, null, 1, 0);
        UUID finde = tarifaDeTipo(negocioA, t, "Fin de semana", "150000", "0", null, null,
                List.of(5, 6, 7), 1, 8);

        // 2026-05-14 es jueves; 15 viernes; 16 sábado
        CotizacionDeEstadia cot = cotizar(negocioA, r, LocalDate.of(2026, 5, 14), 3, 1);

        assertThat(cot.noches()).extracting(n -> n.tarifaId())
                .containsExactly(base, finde, finde);
        assertThat(cot.total()).isEqualByComparingTo("400000");
    }

    @Test
    @DisplayName("Criterio 5: una tarifa con estancia mínima de dos noches no entra en una estancia de una")
    void estanciaMinimaExcluyeLaTarifa() {
        UUID t = tipo(negocioA, "Loft");
        UUID r = recurso(negocioA, t, "L1");
        UUID base = tarifaDeTipo(negocioA, t, "Base", "100000", "0", null, null, null, 1, 0);
        UUID larga = tarifaDeTipo(negocioA, t, "Estadía larga", "80000", "0", null, null, null,
                2, 20);

        CotizacionDeEstadia unaNoche = cotizar(negocioA, r, LocalDate.of(2026, 5, 16), 1, 1);
        assertThat(unaNoche.noches().get(0).tarifaId()).isEqualTo(base);
        assertThat(unaNoche.total()).isEqualByComparingTo("100000");

        CotizacionDeEstadia dosNoches = cotizar(negocioA, r, LocalDate.of(2026, 5, 16), 2, 1);
        assertThat(dosNoches.noches()).extracting(n -> n.tarifaId()).containsExactly(larga, larga);
        assertThat(dosNoches.total()).isEqualByComparingTo("160000");
    }

    @Test
    @DisplayName("La persona adicional se cobra en cada noche a partir de la segunda persona")
    void personaAdicionalPorNoche() {
        UUID t = tipo(negocioA, "Familiar");
        UUID r = recurso(negocioA, t, "F1");
        tarifaDeTipo(negocioA, t, "Base", "100000", "20000", null, null, null, 1, 0);

        CotizacionDeEstadia cot = cotizar(negocioA, r, LocalDate.of(2026, 5, 16), 2, 3);

        assertThat(cot.personas()).isEqualTo(3);
        assertThat(cot.noches()).extracting(n -> n.precio())
                .containsExactly(new BigDecimal("140000.0000"), new BigDecimal("140000.0000"));
        assertThat(cot.total()).isEqualByComparingTo("280000");
    }

    @Test
    @DisplayName("Si ninguna tarifa aplica a una noche, la cotización queda incompleta y esa noche vale 0")
    void nocheSinTarifaDejaLaCotizacionIncompleta() {
        UUID t = tipo(negocioA, "Glamping");
        UUID r = recurso(negocioA, t, "G1");
        tarifaDeTipo(negocioA, t, "Fin de semana", "150000", "0", null, null, List.of(5, 6, 7),
                1, 0);

        // 2026-05-18 es lunes
        CotizacionDeEstadia cot = cotizar(negocioA, r, LocalDate.of(2026, 5, 18), 1, 1);

        assertThat(cot.completa()).isFalse();
        assertThat(cot.noches().get(0).tarifaId()).isNull();
        assertThat(cot.noches().get(0).precio()).isEqualByComparingTo("0");
        assertThat(cot.total()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("El segundo negocio nunca ve las tarifas ni los recursos del primero")
    void aislamientoEntreNegocios() {
        UUID tipoA = tipo(negocioA, "Habitación");
        UUID recursoA = recurso(negocioA, tipoA, "101");
        tarifaDeTipo(negocioA, tipoA, "Base A", "100000", "0", null, null, null, 1, 0);

        UUID tipoB = tipo(negocioB, "Habitación");
        UUID recursoB = recurso(negocioB, tipoB, "101");
        UUID tarifaB = tarifaDeTipo(negocioB, tipoB, "Base B", "40000", "0", null, null, null, 1, 0);

        CotizacionDeEstadia cot = cotizar(negocioB, recursoB, LocalDate.of(2026, 5, 16), 2, 1);
        assertThat(cot.noches()).extracting(n -> n.tarifaId()).containsExactly(tarifaB, tarifaB);
        assertThat(cot.total()).isEqualByComparingTo("80000");
        assertThat(cot.moneda()).isEqualTo("COP");

        assertThatThrownBy(() -> cotizar(negocioB, recursoA, LocalDate.of(2026, 5, 16), 1, 1))
                .isInstanceOf(NoEncontradoException.class);

        assertThat(enContexto(negocioB, admin, ADMIN, () -> tarifas.listar()))
                .extracting(TarifaDelNegocio::nombre)
                .containsExactly("Base B");
    }
}
