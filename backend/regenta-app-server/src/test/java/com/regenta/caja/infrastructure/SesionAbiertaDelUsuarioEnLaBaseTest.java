package com.regenta.caja.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;

import com.regenta.PostgresDePrueba;
import com.regenta.caja.domain.EstadoSesionCaja;

/** HU-146 C7 · la sesión abierta de un usuario se busca solo en su negocio. */
@DataJpaTest
class SesionAbiertaDelUsuarioEnLaBaseTest extends PostgresDePrueba {

    @Autowired
    private SesionDeCajaRepositorio sesiones;
    @Autowired
    private JdbcTemplate jdbc;

    @Test
    @DisplayName("HU-146 C7 · Dados dos negocios, la sesión abierta del negocio A nunca se usa para el negocio B")
    void laSesionDeOtroNegocioNoSeEncuentra() {
        UUID negocioA = UUID.randomUUID();
        UUID negocioB = UUID.randomUUID();
        UUID usuario = UUID.randomUUID();   // el mismo id, a propósito, en los dos negocios
        UUID sesionA = sesionAbierta(negocioA, usuario);

        comoElNegocio(jdbc, negocioB);
        assertThat(sesiones.findFirstByNegocioIdAndUsuarioAperturaIdAndEstadoOrderByAbiertaEnDesc(
                negocioB, usuario, EstadoSesionCaja.ABIERTA)).isEmpty();
        assertThat(sesiones.findFirstByNegocioIdAndUsuarioAperturaIdAndEstadoOrderByAbiertaEnDesc(
                negocioA, usuario, EstadoSesionCaja.ABIERTA)).isEmpty();

        comoElNegocio(jdbc, negocioA);
        assertThat(sesiones.findFirstByNegocioIdAndUsuarioAperturaIdAndEstadoOrderByAbiertaEnDesc(
                negocioA, usuario, EstadoSesionCaja.ABIERTA))
                .get().extracting(s -> s.getId()).isEqualTo(sesionA);
    }

    private UUID sesionAbierta(UUID negocio, UUID usuario) {
        comoElNegocio(jdbc, negocio);
        UUID caja = UUID.randomUUID();
        UUID sesion = UUID.randomUUID();
        jdbc.update("insert into caja.cajas (id, negocio_id, codigo, nombre) values (?, ?, ?, 'Caja')",
                caja, negocio, "C-" + caja.toString().substring(0, 8));
        jdbc.update("""
                insert into caja.sesiones_caja (id, negocio_id, caja_id, numero, usuario_apertura_id)
                values (?, ?, ?, ?, ?)
                """, sesion, negocio, caja, "CAJA-" + sesion.toString().substring(0, 8), usuario);
        return sesion;
    }
}
