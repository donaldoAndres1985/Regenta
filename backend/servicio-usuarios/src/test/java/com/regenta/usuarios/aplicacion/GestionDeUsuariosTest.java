package com.regenta.usuarios.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.regenta.comun.errores.LimiteDePlanException;
import com.regenta.comun.errores.NoAutenticadoException;
import com.regenta.comun.errores.RecursoDuplicadoException;
import com.regenta.comun.errores.RecursoExpiradoException;
import com.regenta.comun.errores.SinPermisoException;
import com.regenta.usuarios.BaseDeUsuarios;
import com.regenta.usuarios.domain.Patrones;
import com.regenta.usuarios.domain.Usuario;

/** HU-015. Invitar, aceptar, editar y desactivar, con el plan poniendo el techo. */
class GestionDeUsuariosTest extends BaseDeUsuarios {

    private static final AtomicInteger CONSECUTIVO = new AtomicInteger(3000);
    private static final String CLAVE = "clave-de-prueba-larga";
    private static final Set<String> DE_ADMINISTRADOR = Set.of("USUARIOS_USUARIO_VER",
            "USUARIOS_USUARIO_CREAR", "USUARIOS_USUARIO_EDITAR");

    @Autowired
    private AltaDeNegocios alta;

    @Autowired
    private GestionDeUsuarios gestion;

    @Autowired
    private Autenticacion autenticacion;

    private NegocioCreado negocioNuevo(String plan) {
        String documento = "6001234" + CONSECUTIVO.incrementAndGet();
        return alta.registrar(new SolicitudDeAlta("Tienda " + documento, null, "NIT", documento,
                "1", Patrones.VENTA_DIRECTA, plan, "CO", "America/Bogota", "COP", "es-CO",
                new SolicitudDeAlta.Administrador("jefe" + documento + "@regenta.co", "Ana",
                        "Restrepo", CLAVE)));
    }

    private static String correoNuevo() {
        return "empleado" + CONSECUTIVO.incrementAndGet() + "@regenta.co";
    }

    private UUID rolNoAdministrador(NegocioCreado negocio) {
        return UUID.fromString(consultar("select id from roles where negocio_id = '"
                + negocio.negocioId() + "' and not es_sistema order by nombre").get(0));
    }

    private InvitacionEmitida invitar(NegocioCreado negocio, String correo) {
        return enContexto(negocio.negocioId(), negocio.administradorId(), DE_ADMINISTRADOR,
                () -> gestion.invitar(new SolicitudDeInvitacion(correo, "Luis", "Marin",
                        rolNoAdministrador(negocio), List.of())));
    }

    @Test
    @DisplayName("Criterio 3: el invitado acepta y queda ACTIVO, con su rol y pudiendo entrar")
    void elInvitadoAceptaYEntra() {
        NegocioCreado negocio = negocioNuevo("PROFESIONAL");
        String correo = correoNuevo();

        InvitacionEmitida invitacion = invitar(negocio, correo);
        assertThat(consultar("select estado from usuarios where id = '" + invitacion.usuarioId()
                + "'")).containsExactly(Usuario.INVITADO);

        UsuarioDelNegocio aceptado = gestion.aceptarInvitacion(
                new AceptacionDeInvitacion(invitacion.token(), "otra-clave-larga"));

        assertThat(aceptado.estado()).isEqualTo(Usuario.ACTIVO);
        assertThat(aceptado.roles()).hasSize(1);
        assertThat(consultar("select estado from invitaciones where id = '"
                + invitacion.invitacionId() + "'")).containsExactly("ACEPTADA");

        ResultadoDeLogin entrada = autenticacion.entrar(
                new CredencialesDeAcceso(correo, "otra-clave-larga", null, null, null));
        assertThat(entrada.tokenDeAcceso()).isNotBlank();
    }

    @Test
    @DisplayName("Criterio 2: un correo que ya esta en el negocio responde 409")
    void elCorreoRepetidoNoSeInvitaDosVeces() {
        NegocioCreado negocio = negocioNuevo("PROFESIONAL");
        String correo = correoNuevo();
        invitar(negocio, correo);

        assertThatThrownBy(() -> invitar(negocio, correo))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessageContaining(correo);
    }

    @Test
    @DisplayName("Criterio 1: con el cupo del plan lleno, la siguiente invitacion responde 402")
    void elPlanPoneElTecho() {
        NegocioCreado negocio = negocioNuevo("BASICO");

        // El plan Basico admite dos usuarios y el administrador ya ocupa uno.
        invitar(negocio, correoNuevo());

        assertThatThrownBy(() -> invitar(negocio, correoNuevo()))
                .isInstanceOf(LimiteDePlanException.class)
                .hasMessageContaining("BASICO");
    }

    @Test
    @DisplayName("Criterio 1: los invitados sin aceptar tambien ocupan cupo")
    void elInvitadoSinAceptarYaOcupa() {
        NegocioCreado negocio = negocioNuevo("BASICO");
        InvitacionEmitida pendiente = invitar(negocio, correoNuevo());

        assertThat(consultar("select estado from usuarios where id = '" + pendiente.usuarioId()
                + "'")).containsExactly(Usuario.INVITADO);
        assertThatThrownBy(() -> invitar(negocio, correoNuevo()))
                .as("si no contaran, invitar a veinte seria la forma de saltarse el limite")
                .isInstanceOf(LimiteDePlanException.class);
    }

    @Test
    @DisplayName("Criterio 4: una invitacion vencida responde 410")
    void laInvitacionVencidaNoSirve() {
        NegocioCreado negocio = negocioNuevo("PROFESIONAL");
        InvitacionEmitida invitacion = invitar(negocio, correoNuevo());
        comoSuperusuario("UPDATE core_identidad.invitaciones SET expira_en = now() - interval '1 day'"
                + " WHERE id = '" + invitacion.invitacionId() + "'");

        assertThatThrownBy(() -> gestion.aceptarInvitacion(
                new AceptacionDeInvitacion(invitacion.token(), "otra-clave-larga")))
                .isInstanceOf(RecursoExpiradoException.class);

        assertThat(consultar("select estado from invitaciones where id = '"
                + invitacion.invitacionId() + "'")).containsExactly("EXPIRADA");
    }

    @Test
    @DisplayName("Una invitacion ya aceptada no se acepta dos veces")
    void laInvitacionNoSeReutiliza() {
        NegocioCreado negocio = negocioNuevo("PROFESIONAL");
        InvitacionEmitida invitacion = invitar(negocio, correoNuevo());
        gestion.aceptarInvitacion(new AceptacionDeInvitacion(invitacion.token(), "otra-clave-larga"));

        assertThatThrownBy(() -> gestion.aceptarInvitacion(
                new AceptacionDeInvitacion(invitacion.token(), "otra-mas-larga")))
                .isInstanceOf(RecursoExpiradoException.class);
    }

    @Test
    @DisplayName("Criterio 5: al desactivar, el usuario no entra pero sigue existiendo")
    void elDesactivadoNoEntraPeroNoDesaparece() {
        NegocioCreado negocio = negocioNuevo("PROFESIONAL");
        String correo = correoNuevo();
        InvitacionEmitida invitacion = invitar(negocio, correo);
        gestion.aceptarInvitacion(new AceptacionDeInvitacion(invitacion.token(), "otra-clave-larga"));

        enContexto(negocio.negocioId(), negocio.administradorId(), DE_ADMINISTRADOR,
                () -> gestion.desactivar(invitacion.usuarioId()));

        assertThatThrownBy(() -> autenticacion.entrar(
                new CredencialesDeAcceso(correo, "otra-clave-larga", null, null, null)))
                .isInstanceOf(NoAutenticadoException.class);
        assertThat(contar("select count(*) from usuarios where id = '" + invitacion.usuarioId()
                + "'")).as("nunca se borra: sus ventas siguen atribuidas a el").isOne();
        assertThat(consultar("select estado from usuarios where id = '" + invitacion.usuarioId()
                + "'")).containsExactly(Usuario.INACTIVO);
    }

    @Test
    @DisplayName("Sin el permiso de crear usuarios, invitar responde 403")
    void sinPermisoNoSeInvita() {
        NegocioCreado negocio = negocioNuevo("PROFESIONAL");

        assertThatThrownBy(() -> enContexto(negocio.negocioId(), negocio.administradorId(),
                Set.of("USUARIOS_USUARIO_VER"),
                () -> gestion.invitar(new SolicitudDeInvitacion(correoNuevo(), "Luis", null,
                        rolNoAdministrador(negocio), List.of()))))
                .isInstanceOf(SinPermisoException.class)
                .hasMessageContaining("USUARIOS_USUARIO_CREAR");
    }

    @Test
    @DisplayName("Nadie se desactiva a si mismo y se queda sin poder volver a entrar")
    void unoNoSeDesactivaASiMismo() {
        NegocioCreado negocio = negocioNuevo("PROFESIONAL");

        assertThatThrownBy(() -> enContexto(negocio.negocioId(), negocio.administradorId(),
                DE_ADMINISTRADOR, () -> gestion.desactivar(negocio.administradorId())))
                .hasMessageContaining("si mismo");
    }
}
