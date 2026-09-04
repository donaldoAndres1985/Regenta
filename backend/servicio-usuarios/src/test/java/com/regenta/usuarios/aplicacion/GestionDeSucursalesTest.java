package com.regenta.usuarios.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.regenta.comun.errores.LimiteDePlanException;
import com.regenta.comun.errores.RecursoDuplicadoException;
import com.regenta.comun.errores.ReglaDeNegocioException;
import com.regenta.usuarios.BaseDeUsuarios;
import com.regenta.usuarios.domain.Patrones;

/** HU-019. Multi-sucursal se vende aparte, pero la columna existe desde el dia uno. */
class GestionDeSucursalesTest extends BaseDeUsuarios {

    private static final AtomicInteger CONSECUTIVO = new AtomicInteger(9000);
    private static final Set<String> DE_ADMINISTRADOR = Set.of("CONFIGURACION_SUCURSAL_VER",
            "CONFIGURACION_SUCURSAL_CREAR", "CONFIGURACION_SUCURSAL_EDITAR");

    @Autowired
    private AltaDeNegocios alta;

    @Autowired
    private GestionDeSucursales gestion;

    private NegocioCreado negocioNuevo(String plan) {
        String documento = "2001234" + CONSECUTIVO.incrementAndGet();
        return alta.registrar(new SolicitudDeAlta("Cadena " + documento, null, "NIT", documento,
                "1", Patrones.VENTA_DIRECTA, plan, "CO", "America/Bogota", "COP", "es-CO",
                new SolicitudDeAlta.Administrador("jefe" + documento + "@regenta.co", "Ana",
                        "Restrepo", "clave-de-prueba-larga")));
    }

    private <T> T comoAdmin(NegocioCreado negocio, java.util.function.Supplier<T> tarea) {
        return enContexto(negocio.negocioId(), negocio.administradorId(), DE_ADMINISTRADOR, tarea);
    }

    private static SolicitudDeSucursal sucursal(String codigo, boolean principal) {
        return new SolicitudDeSucursal(codigo, "Sede " + codigo, "Calle 1", "Medellin",
                "6040000000", principal);
    }

    @Test
    @DisplayName("Criterio 2: el negocio nace con una sola sucursal, y es la principal")
    void naceConLaPrincipal() {
        NegocioCreado negocio = negocioNuevo("PROFESIONAL");

        assertThat(comoAdmin(negocio, gestion::listar))
                .singleElement()
                .satisfies(sucursal -> {
                    assertThat(sucursal.codigo()).isEqualTo("001");
                    assertThat(sucursal.esPrincipal()).isTrue();
                });
    }

    @Test
    @DisplayName("Criterio 1: sin multi-sucursal, la segunda responde 402 y no 403")
    void sinMultiSucursalNoHaySegunda() {
        NegocioCreado negocio = negocioNuevo("PROFESIONAL");

        assertThatThrownBy(() -> comoAdmin(negocio, () -> gestion.crear(sucursal("002", false))))
                .isInstanceOf(LimiteDePlanException.class)
                .hasMessageContaining("MULTISUCURSAL");
    }

    @Test
    @DisplayName("Con plan Empresarial si se abre otra")
    void conEmpresarialSeAbreOtra() {
        NegocioCreado negocio = negocioNuevo("EMPRESARIAL");

        SucursalDelNegocio segunda = comoAdmin(negocio, () -> gestion.crear(sucursal("002", false)));

        assertThat(segunda.codigo()).isEqualTo("002");
        assertThat(segunda.esPrincipal()).isFalse();
        assertThat(comoAdmin(negocio, gestion::listar)).hasSize(2);
        assertThat(consultar("select tipo_evento from outbox_eventos where negocio_id = '"
                + negocio.negocioId() + "'")).contains("sucursal_creada");
    }

    @Test
    @DisplayName("Criterio 3: al marcar otra principal, la anterior deja de serlo")
    void soloPuedeHaberUnaPrincipal() {
        NegocioCreado negocio = negocioNuevo("EMPRESARIAL");
        SucursalDelNegocio segunda = comoAdmin(negocio, () -> gestion.crear(sucursal("002", false)));

        comoAdmin(negocio, () -> gestion.marcarPrincipal(segunda.id()));

        assertThat(consultar("select codigo from sucursales where negocio_id = '"
                + negocio.negocioId() + "' and es_principal"))
                .as("el indice unico parcial no admite dos, ni por un instante")
                .containsExactly("002");
        assertThat(comoAdmin(negocio, gestion::listar))
                .filteredOn(SucursalDelNegocio::esPrincipal)
                .hasSize(1);
    }

    @Test
    @DisplayName("Crear una sucursal marcada como principal desplaza a la anterior")
    void laNuevaPrincipalDesplazaALaVieja() {
        NegocioCreado negocio = negocioNuevo("EMPRESARIAL");

        SucursalDelNegocio nueva = comoAdmin(negocio, () -> gestion.crear(sucursal("002", true)));

        assertThat(nueva.esPrincipal()).isTrue();
        assertThat(consultar("select codigo from sucursales where negocio_id = '"
                + negocio.negocioId() + "' and es_principal")).containsExactly("002");
    }

    @Test
    @DisplayName("El codigo de sucursal no se repite dentro del negocio")
    void elCodigoNoSeRepite() {
        NegocioCreado negocio = negocioNuevo("EMPRESARIAL");

        assertThatThrownBy(() -> comoAdmin(negocio, () -> gestion.crear(sucursal("001", false))))
                .isInstanceOf(RecursoDuplicadoException.class);
    }

    @Test
    @DisplayName("La principal no se desactiva: primero hay que nombrar otra")
    void laPrincipalNoSeDesactiva() {
        NegocioCreado negocio = negocioNuevo("EMPRESARIAL");

        assertThatThrownBy(() -> comoAdmin(negocio, () -> {
            gestion.desactivar(negocio.sucursalPrincipalId());
            return null;
        })).isInstanceOf(ReglaDeNegocioException.class);
    }
}
