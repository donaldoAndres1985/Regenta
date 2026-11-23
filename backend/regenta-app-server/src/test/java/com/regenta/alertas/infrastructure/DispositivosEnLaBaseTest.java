package com.regenta.alertas.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;

import com.regenta.PostgresDePrueba;
import com.regenta.alertas.application.DispositivoDelUsuario;
import com.regenta.alertas.application.GestionDeDispositivos;
import com.regenta.alertas.application.SolicitudDeDispositivo;
import com.regenta.alertas.domain.DispositivoPush;
import com.regenta.shared.tenancy.ContextoDeNegocio;
import com.regenta.shared.tenancy.DatosDelNegocio;

/**
 * HU-123 C1 contra PostgreSQL (alertas V6) · El token FCM queda contra el usuario y el negocio. El mismo
 * celular se registra en dos negocios sin chocar (con RLS, uno no ve los dispositivos del otro), y volver a
 * registrar un token que FCM había dado por muerto lo reactiva.
 */
@DataJpaTest
class DispositivosEnLaBaseTest extends PostgresDePrueba {

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private DispositivoPushRepositorio dispositivos;

    private final UUID negocioA = UUID.randomUUID();
    private final UUID negocioB = UUID.randomUUID();
    private final UUID usuario = UUID.randomUUID();

    @AfterEach
    void limpiar() {
        ContextoDeNegocio.limpiar();
    }

    private GestionDeDispositivos en(UUID negocio) {
        ContextoDeNegocio.establecer(new DatosDelNegocio(negocio, usuario, "", "", Set.of("ADMINISTRADOR"),
                Set.of("ALERTAS"), Set.of("ALERTAS_ALERTA_VER"), Set.of()));
        comoElNegocio(jdbc, negocio);
        return new GestionDeDispositivos(dispositivos);
    }

    @Test
    void c1_elTokenQuedaContraElUsuarioYElNegocio() {
        DispositivoDelUsuario d = en(negocioA).registrar(
                new SolicitudDeDispositivo("token-del-celular", "ANDROID", "Pixel 8", "1.4.0"));
        dispositivos.flush();

        assertThat(jdbc.queryForObject("SELECT count(*) FROM alertas.dispositivos_push WHERE token_fcm = ? "
                + "AND negocio_id = ? AND usuario_id = ? AND activo", Integer.class, "token-del-celular", negocioA,
                usuario)).isEqualTo(1);
        assertThat(d.id()).isNotNull();
    }

    @Test
    void c1_elMismoCelularEnDosNegociosNoChoca() {
        en(negocioA).registrar(new SolicitudDeDispositivo("token-compartido", "ANDROID", null, null));
        dispositivos.flush();

        en(negocioB).registrar(new SolicitudDeDispositivo("token-compartido", "ANDROID", null, null));
        dispositivos.flush();

        assertThat(dispositivos.findByNegocioIdAndUsuarioIdAndActivoTrue(negocioB, usuario))
                .extracting(DispositivoPush::getTokenFcm).containsExactly("token-compartido");
    }

    @Test
    void c3_volverARegistrarUnTokenMuertoLoReactiva() {
        GestionDeDispositivos gestion = en(negocioA);
        gestion.registrar(new SolicitudDeDispositivo("token-x", "ANDROID", null, null));
        dispositivos.flush();
        DispositivoPush d = dispositivos.findByNegocioIdAndTokenFcm(negocioA, "token-x").orElseThrow();
        d.desactivar();
        dispositivos.saveAndFlush(d);

        gestion.registrar(new SolicitudDeDispositivo("token-x", "ANDROID", null, null));
        dispositivos.flush();

        assertThat(dispositivos.findByNegocioIdAndUsuarioIdAndActivoTrue(negocioA, usuario)).hasSize(1);
    }
}
