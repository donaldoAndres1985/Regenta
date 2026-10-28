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

import com.regenta.comun.errores.SinPermisoException;
import com.regenta.usuarios.BaseDeUsuarios;
import com.regenta.usuarios.domain.Patrones;

/**
 * HU-138 criterio 8, lado proveedor. Los usuarios activos de un negocio que
 * tienen un rol: a quién le llega una alerta dirigida a ese rol (HU-092
 * criterio 5). El rol se nombra igual que en el token: su nombre en mayúsculas
 * y con guion bajo. Con dos negocios cargados.
 */
class UsuariosConRolTest extends BaseDeUsuarios {

    private static final AtomicInteger CONSECUTIVO = new AtomicInteger(7000);
    private static final String CLAVE = "clave-de-prueba-larga";
    private static final Set<String> ADMIN = Set.of("USUARIOS_USUARIO_VER",
            "USUARIOS_USUARIO_CREAR", "USUARIOS_USUARIO_EDITAR");
    private static final Set<String> VER = Set.of("USUARIOS_USUARIO_VER");

    @Autowired
    private AltaDeNegocios alta;
    @Autowired
    private GestionDeUsuarios gestion;

    private NegocioCreado negocioNuevo() {
        String documento = "7001234" + CONSECUTIVO.incrementAndGet();
        return alta.registrar(new SolicitudDeAlta("Tienda " + documento, null, "NIT", documento,
                "1", Patrones.VENTA_DIRECTA, "PROFESIONAL", "CO", "America/Bogota", "COP", "es-CO",
                new SolicitudDeAlta.Administrador("jefe" + documento + "@regenta.co", "Ana",
                        "Restrepo", CLAVE)));
    }

    /** El primer rol que no es de sistema, con el código con que viaja en el token. */
    private String[] otroRol(NegocioCreado negocio) {
        List<String> fila = consultar("select id || '|' || nombre from roles where negocio_id = '"
                + negocio.negocioId() + "' and not es_sistema order by nombre");
        String[] partes = fila.get(0).split("\\|");
        return new String[] {partes[0], partes[1].toUpperCase().replace(' ', '_')};
    }

    private UUID empleado(NegocioCreado negocio, UUID rolId, boolean acepta) {
        InvitacionEmitida invitacion = enContexto(negocio.negocioId(), negocio.administradorId(),
                ADMIN, () -> gestion.invitar(new SolicitudDeInvitacion(
                        "empleado" + CONSECUTIVO.incrementAndGet() + "@regenta.co", "Luis",
                        "Marin", rolId, List.of())));
        if (acepta) {
            gestion.aceptarInvitacion(new AceptacionDeInvitacion(invitacion.token(), CLAVE));
        }
        return invitacion.usuarioId();
    }

    private List<UUID> conRol(NegocioCreado negocio, String rol) {
        return enContexto(negocio.negocioId(), null, VER, () -> gestion.conRol(rol));
    }

    @Test
    @DisplayName("Criterio 8: devuelve los usuarios activos que tienen ese rol")
    void usuariosActivosConElRol() {
        NegocioCreado negocio = negocioNuevo();
        String[] rol = otroRol(negocio);
        UUID activo = empleado(negocio, UUID.fromString(rol[0]), true);

        assertThat(conRol(negocio, rol[1])).containsExactly(activo);
        assertThat(conRol(negocio, "ADMINISTRADOR")).containsExactly(negocio.administradorId());
    }

    @Test
    @DisplayName("Criterio 8: el código del rol no distingue mayúsculas")
    void sinDistinguirMayusculas() {
        NegocioCreado negocio = negocioNuevo();

        assertThat(conRol(negocio, "administrador")).containsExactly(negocio.administradorId());
    }

    @Test
    @DisplayName("Criterio 8: un invitado que no ha aceptado o un usuario desactivado no recibe alertas")
    void soloActivos() {
        NegocioCreado negocio = negocioNuevo();
        String[] rol = otroRol(negocio);
        empleado(negocio, UUID.fromString(rol[0]), false);
        UUID desactivado = empleado(negocio, UUID.fromString(rol[0]), true);
        enContexto(negocio.negocioId(), negocio.administradorId(), ADMIN,
                () -> gestion.desactivar(desactivado));

        assertThat(conRol(negocio, rol[1])).isEmpty();
    }

    @Test
    @DisplayName("Criterio 8: un rol que el negocio no tiene no devuelve a nadie")
    void rolInexistente() {
        assertThat(conRol(negocioNuevo(), "ASTRONAUTA")).isEmpty();
    }

    @Test
    @DisplayName("Criterio 9: los usuarios del negocio B no aparecen en las alertas del negocio A")
    void aislamientoEntreNegocios() {
        NegocioCreado a = negocioNuevo();
        NegocioCreado b = negocioNuevo();

        assertThat(conRol(a, "ADMINISTRADOR")).containsExactly(a.administradorId())
                .doesNotContain(b.administradorId());
    }

    @Test
    @DisplayName("Sin permiso de ver usuarios no se consulta")
    void sinPermiso() {
        NegocioCreado negocio = negocioNuevo();

        assertThatThrownBy(() -> enContexto(negocio.negocioId(), null, Set.of(),
                () -> gestion.conRol("ADMINISTRADOR"))).isInstanceOf(SinPermisoException.class);
    }
}
