package com.regenta.caja.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;

import com.regenta.PostgresDePrueba;
import com.regenta.caja.domain.SesionDeCaja;

/** HU-143 · El buscador de cierres contra PostgreSQL (caja V11): rango, cajero, páginas y RLS. */
@DataJpaTest
class ConsultaDeCierresEnLaBaseTest extends PostgresDePrueba {

    @Autowired
    private SesionDeCajaRepositorio sesiones;
    @Autowired
    private JdbcTemplate jdbc;

    private final UUID negocioA = UUID.randomUUID();
    private final UUID negocioB = UUID.randomUUID();
    private final UUID ana = UUID.randomUUID();
    private final UUID luis = UUID.randomUUID();
    private final OffsetDateTime dia = OffsetDateTime.parse("2026-11-20T18:00-05:00");

    private UUID caja(UUID negocio) {
        comoElNegocio(jdbc, negocio);
        UUID caja = UUID.randomUUID();
        jdbc.update("insert into caja.cajas (id, negocio_id, codigo, nombre) values (?, ?, ?, 'Caja')", caja, negocio,
                "C-" + caja.toString().substring(0, 8));
        return caja;
    }

    private void sesion(UUID negocio, UUID caja, String numero, UUID abre, UUID cierra, OffsetDateTime cerradaEn,
            String estado) {
        comoElNegocio(jdbc, negocio);
        jdbc.update("""
                insert into caja.sesiones_caja (id, negocio_id, caja_id, numero, usuario_apertura_id,
                    usuario_cierre_id, abierta_en, cerrada_en, estado)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, UUID.randomUUID(), negocio, caja, numero, abre, cierra,
                cerradaEn == null ? dia.minusHours(9) : cerradaEn.minusHours(9), cerradaEn, estado);
    }

    @Test
    @DisplayName("C1 · Los cierres del rango, el más reciente primero, por páginas; las abiertas no son cierres")
    void rangoYPaginas() {
        UUID caja = caja(negocioA);
        sesion(negocioA, caja, "CAJA-1", ana, ana, dia.minusDays(2), "CUADRADA");
        sesion(negocioA, caja, "CAJA-2", ana, luis, dia.minusDays(1), "DESCUADRADA");
        sesion(negocioA, caja, "CAJA-3", luis, luis, dia, "CUADRADA");
        sesion(negocioA, caja, "CAJA-4", ana, ana, dia.minusDays(30), "CUADRADA");
        sesion(negocioA, caja(negocioA), "CAJA-5", ana, null, null, "ABIERTA");

        comoElNegocio(jdbc, negocioA);
        Page<SesionDeCaja> primera = sesiones.cierres(negocioA, dia.minusDays(5), dia.plusDays(1), null,
                PageRequest.of(0, 2));
        assertThat(primera.getContent()).extracting(SesionDeCaja::getNumero).containsExactly("CAJA-3", "CAJA-2");
        assertThat(primera.getTotalElements()).isEqualTo(3);
        assertThat(primera.hasNext()).isTrue();
        Page<SesionDeCaja> segunda = sesiones.cierres(negocioA, dia.minusDays(5), dia.plusDays(1), null,
                PageRequest.of(1, 2));
        assertThat(segunda.getContent()).extracting(SesionDeCaja::getNumero).containsExactly("CAJA-1");
        assertThat(segunda.hasNext()).isFalse();
    }

    @Test
    @DisplayName("C1 · Filtrar por cajero trae los turnos que abrió o cerró")
    void porCajero() {
        UUID caja = caja(negocioA);
        sesion(negocioA, caja, "CAJA-1", ana, ana, dia.minusDays(2), "CUADRADA");
        sesion(negocioA, caja, "CAJA-2", ana, luis, dia.minusDays(1), "DESCUADRADA");
        sesion(negocioA, caja, "CAJA-3", luis, luis, dia, "CUADRADA");

        comoElNegocio(jdbc, negocioA);
        assertThat(sesiones.cierres(negocioA, dia.minusDays(5), dia.plusDays(1), ana, PageRequest.of(0, 20))
                .getContent()).extracting(SesionDeCaja::getNumero).containsExactly("CAJA-2", "CAJA-1");
        assertThat(sesiones.cierres(negocioA, dia.minusDays(5), dia.plusDays(1), luis, PageRequest.of(0, 20))
                .getContent()).extracting(SesionDeCaja::getNumero).containsExactly("CAJA-3", "CAJA-2");
        assertThat(sesiones.cajerosDelNegocio(negocioA)).containsExactlyInAnyOrder(ana, luis);
    }

    @Test
    @DisplayName("C6 · Dados dos negocios, el negocio B no ve los cierres del negocio A")
    void otroNegocio() {
        sesion(negocioA, caja(negocioA), "CAJA-1", ana, ana, dia, "CUADRADA");
        sesion(negocioB, caja(negocioB), "CAJA-1", luis, luis, dia, "CUADRADA");

        comoElNegocio(jdbc, negocioB);
        // Aunque pidiera los del negocio A, la RLS solo le deja ver los suyos.
        assertThat(sesiones.cierres(negocioA, dia.minusDays(1), dia.plusDays(1), null, PageRequest.of(0, 20))
                .getContent()).isEmpty();
        assertThat(sesiones.cierres(negocioB, dia.minusDays(1), dia.plusDays(1), null, PageRequest.of(0, 20))
                .getContent()).singleElement().satisfies(s -> assertThat(s.getUsuarioAperturaId()).isEqualTo(luis));
        assertThat(sesiones.cajerosDelNegocio(negocioA)).isEmpty();
    }
}
