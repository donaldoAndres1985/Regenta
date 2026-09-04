package com.regenta.usuarios.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.text.ParseException;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.nimbusds.jwt.SignedJWT;
import com.regenta.comun.errores.ConflictoDeEstadoException;
import com.regenta.comun.errores.RecursoDuplicadoException;
import com.regenta.comun.errores.ReglaDeNegocioException;
import com.regenta.comun.errores.SinPermisoException;
import com.regenta.usuarios.BaseDeUsuarios;
import com.regenta.usuarios.domain.Patrones;

/** HU-016. El motor de roles: el Core solo conoce rol y permiso. */
class GestionDeRolesTest extends BaseDeUsuarios {

    private static final AtomicInteger CONSECUTIVO = new AtomicInteger(4000);
    private static final String CLAVE = "clave-de-prueba-larga";
    private static final Set<String> DE_ADMINISTRADOR = Set.of("USUARIOS_ROL_VER",
            "USUARIOS_ROL_CREAR", "USUARIOS_ROL_EDITAR", "USUARIOS_ROL_ELIMINAR",
            "USUARIOS_USUARIO_EDITAR");

    @Autowired
    private AltaDeNegocios alta;

    @Autowired
    private GestionDeRoles gestion;

    @Autowired
    private GestionDeUsuarios usuarios;

    @Autowired
    private Autenticacion autenticacion;

    private String correo;

    private NegocioCreado negocioNuevo() {
        String documento = "5001234" + CONSECUTIVO.incrementAndGet();
        correo = "jefe" + documento + "@regenta.co";
        return alta.registrar(new SolicitudDeAlta("Tienda " + documento, null, "NIT", documento,
                "1", Patrones.VENTA_DIRECTA, "PROFESIONAL", "CO", "America/Bogota", "COP", "es-CO",
                new SolicitudDeAlta.Administrador(correo, "Ana", "Restrepo", CLAVE)));
    }

    private <T> T comoAdmin(NegocioCreado negocio, java.util.function.Supplier<T> tarea) {
        return enContexto(negocio.negocioId(), negocio.administradorId(), DE_ADMINISTRADOR, tarea);
    }

    @Test
    @DisplayName("Criterio 1: un rol nuevo lleva el subconjunto de permisos que se le de")
    void unRolLlevaLoQueSeLeDe() {
        NegocioCreado negocio = negocioNuevo();

        RolDelNegocio bodeguero = comoAdmin(negocio, () -> gestion.crear(new SolicitudDeRol(
                "Bodeguero", "Mueve stock y no ve costos",
                List.of("INVENTARIO_PRODUCTO_VER", "INVENTARIO_STOCK_VER",
                        "INVENTARIO_STOCK_EDITAR"))));

        assertThat(bodeguero.permisos()).containsExactly("INVENTARIO_PRODUCTO_VER",
                "INVENTARIO_STOCK_EDITAR", "INVENTARIO_STOCK_VER");
        assertThat(bodeguero.esSistema()).isFalse();
        assertThat(bodeguero.usuariosAsignados()).isZero();
        assertThat(contar("select count(*) from rol_permisos where rol_id = '" + bodeguero.id()
                + "'")).isEqualTo(3);
    }

    @Test
    @DisplayName("Un permiso inventado no pasa: el catalogo es global y cerrado")
    void unPermisoInventadoNoPasa() {
        NegocioCreado negocio = negocioNuevo();

        assertThatThrownBy(() -> comoAdmin(negocio, () -> gestion.crear(new SolicitudDeRol(
                "Todopoderoso", null, List.of("INVENTARIO_PRODUCTO_VER", "BORRAR_TODO")))))
                .isInstanceOf(ReglaDeNegocioException.class);
    }

    @Test
    @DisplayName("Dos roles con el mismo nombre en el mismo negocio, no")
    void elNombreNoSeRepite() {
        NegocioCreado negocio = negocioNuevo();
        comoAdmin(negocio, () -> gestion.crear(new SolicitudDeRol("Bodeguero", null,
                List.of("INVENTARIO_PRODUCTO_VER"))));

        assertThatThrownBy(() -> comoAdmin(negocio, () -> gestion.crear(new SolicitudDeRol(
                "Bodeguero", null, List.of("INVENTARIO_STOCK_VER")))))
                .isInstanceOf(RecursoDuplicadoException.class);
    }

    @Test
    @DisplayName("Criterio 3: el rol de sistema no se elimina")
    void elRolDeSistemaNoSeElimina() {
        NegocioCreado negocio = negocioNuevo();
        UUID administrador = UUID.fromString(consultar("select id from roles where negocio_id = '"
                + negocio.negocioId() + "' and es_sistema").get(0));

        assertThatThrownBy(() -> comoAdmin(negocio, () -> {
            gestion.eliminar(administrador);
            return null;
        })).isInstanceOf(ConflictoDeEstadoException.class)
                .hasMessageContaining("Administrador");
    }

    @Test
    @DisplayName("Criterio 4: un rol con gente asignada exige reasignar primero")
    void elRolConGenteNoSeElimina() {
        NegocioCreado negocio = negocioNuevo();
        RolDelNegocio bodeguero = comoAdmin(negocio, () -> gestion.crear(new SolicitudDeRol(
                "Bodeguero", null, List.of("INVENTARIO_PRODUCTO_VER"))));
        comoAdmin(negocio, () -> usuarios.actualizar(negocio.administradorId(),
                new CambioDeUsuario("Ana", "Restrepo", null, null, List.of(bodeguero.id()), null)));

        assertThatThrownBy(() -> comoAdmin(negocio, () -> {
            gestion.eliminar(bodeguero.id());
            return null;
        })).isInstanceOf(ConflictoDeEstadoException.class)
                .hasMessageContaining("Reasignalos");
    }

    @Test
    @DisplayName("Un rol sin gente si se elimina")
    void elRolSinGenteSeElimina() {
        NegocioCreado negocio = negocioNuevo();
        RolDelNegocio suelto = comoAdmin(negocio, () -> gestion.crear(new SolicitudDeRol(
                "Auxiliar", null, List.of("INVENTARIO_PRODUCTO_VER"))));

        comoAdmin(negocio, () -> {
            gestion.eliminar(suelto.id());
            return null;
        });

        assertThat(contar("select count(*) from roles where id = '" + suelto.id() + "'")).isZero();
    }

    @Test
    @DisplayName("Criterio 2: sin el permiso, el API responde 403 aunque el modulo este activo")
    void sinPermisoResponde403() {
        NegocioCreado negocio = negocioNuevo();

        assertThatThrownBy(() -> enContexto(negocio.negocioId(), negocio.administradorId(),
                Set.of("USUARIOS_ROL_VER"), () -> gestion.crear(new SolicitudDeRol("X", null,
                        List.of("INVENTARIO_PRODUCTO_VER")))))
                .isInstanceOf(SinPermisoException.class)
                .hasMessageContaining("USUARIOS_ROL_CREAR");
    }

    @Test
    @DisplayName("El catalogo de permisos es el global, para poder armar cualquier rol")
    void elCatalogoEsGlobal() {
        NegocioCreado negocio = negocioNuevo();

        List<PermisoDelCatalogo> catalogo = comoAdmin(negocio, gestion::catalogoDePermisos);

        assertThat(catalogo).hasSize((int) contar("select count(*) from permisos"));
        assertThat(catalogo).extracting(PermisoDelCatalogo::codigo)
                .contains("INVENTARIO_PRODUCTO_EDITAR", "VENTAS_VENTA_ANULAR");
    }

    @Test
    @DisplayName("Criterio 5: un usuario acotado a una sucursal solo lleva esa en su token")
    void elAlcancePorSucursalViajaEnElToken() throws ParseException {
        NegocioCreado negocio = negocioNuevo();
        comoAdmin(negocio, () -> usuarios.actualizar(negocio.administradorId(),
                new CambioDeUsuario("Ana", "Restrepo", null, null, null,
                        List.of(negocio.sucursalPrincipalId()))));

        ResultadoDeLogin entrada = autenticacion.entrar(
                new CredencialesDeAcceso(correo, CLAVE, null, null, null));

        SignedJWT token = SignedJWT.parse(entrada.tokenDeAcceso());
        assertThat(token.getJWTClaimsSet().getStringListClaim("sucursales"))
                .containsExactly(negocio.sucursalPrincipalId().toString());
    }
}
