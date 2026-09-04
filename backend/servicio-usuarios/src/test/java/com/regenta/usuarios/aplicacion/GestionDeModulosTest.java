package com.regenta.usuarios.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.text.ParseException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.nimbusds.jwt.SignedJWT;
import com.regenta.comun.errores.ReglaDeNegocioException;
import com.regenta.comun.errores.SinPermisoException;
import com.regenta.usuarios.BaseDeUsuarios;
import com.regenta.usuarios.domain.Patrones;

/** HU-017 (el plan se valida en el backend) y HU-020 (la app sabe que tiene). */
class GestionDeModulosTest extends BaseDeUsuarios {

    private static final AtomicInteger CONSECUTIVO = new AtomicInteger(7000);
    private static final String CLAVE = "clave-de-prueba-larga";
    private static final Set<String> DE_ADMINISTRADOR = Set.of("CONFIGURACION_NEGOCIO_EDITAR");

    @Autowired
    private AltaDeNegocios alta;

    @Autowired
    private GestionDeModulos gestion;

    @Autowired
    private Autenticacion autenticacion;

    private String correo;

    private NegocioCreado negocioNuevo(String plan, String patron) {
        String documento = "4001234" + CONSECUTIVO.incrementAndGet();
        correo = "jefe" + documento + "@regenta.co";
        return alta.registrar(new SolicitudDeAlta("Negocio " + documento, null, "NIT", documento,
                "1", patron, plan, "CO", "America/Bogota", "COP", "es-CO",
                new SolicitudDeAlta.Administrador(correo, "Ana", "Restrepo", CLAVE)));
    }

    private <T> T comoAdmin(NegocioCreado negocio, java.util.function.Supplier<T> tarea) {
        return enContexto(negocio.negocioId(), negocio.administradorId(), DE_ADMINISTRADOR, tarea);
    }

    @Test
    @DisplayName("Criterio 4: FACTURACION sin VENTAS se rechaza, y dice que falta")
    void sinLoQueNecesitaNoSeEnciende() {
        NegocioCreado negocio = negocioNuevo("BASICO", Patrones.COMANDA);

        assertThatThrownBy(() -> comoAdmin(negocio, () -> gestion.activarComoAddon("FACTURACION")))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("VENTAS");
    }

    @Test
    @DisplayName("Criterio 2: un add-on enciende lo que el plan base no trae")
    void elAddonEnciendeSinCambiarDePlan() {
        NegocioCreado negocio = negocioNuevo("BASICO", Patrones.VENTA_DIRECTA);
        assertThat(negocio.modulosActivos()).doesNotContain("CAJA");

        ModuloActivo caja = comoAdmin(negocio, () -> gestion.activarComoAddon("CAJA"));

        assertThat(caja.codigo()).isEqualTo("CAJA");
        assertThat(caja.origen()).isEqualTo("ADDON");
        assertThat(consultar("select plan_id from negocios where id = '" + negocio.negocioId()
                + "'")).as("el plan no cambio: el add-on se paga aparte").hasSize(1);
        assertThat(comoAdmin(negocio, gestion::activos)).extracting(ModuloActivo::codigo)
                .contains("CAJA");
    }

    @Test
    @DisplayName("HU-020 criterio 3: el modulo nuevo aparece en el token al refrescarlo")
    void elModuloNuevoLlegaAlRefrescarElToken() throws ParseException {
        NegocioCreado negocio = negocioNuevo("BASICO", Patrones.VENTA_DIRECTA);
        ResultadoDeLogin entrada = autenticacion.entrar(
                new CredencialesDeAcceso(correo, CLAVE, null, "celular", "ANDROID"));
        assertThat(SignedJWT.parse(entrada.tokenDeAcceso()).getJWTClaimsSet()
                .getStringListClaim("modulos")).doesNotContain("CAJA");

        comoAdmin(negocio, () -> gestion.activarComoAddon("CAJA"));

        ResultadoDeLogin refrescada = autenticacion.refrescar(
                new SolicitudDeRefresco(entrada.tokenDeRefresco(), "celular", "ANDROID"));
        assertThat(SignedJWT.parse(refrescada.tokenDeAcceso()).getJWTClaimsSet()
                .getStringListClaim("modulos"))
                .as("la navegacion se actualiza al refrescar, sin volver a entrar")
                .contains("CAJA");
    }

    @Test
    @DisplayName("Criterio 1: un plan Basico no trae FACTURACION, y por eso el guardia corta")
    void elBasicoNoTraeFacturacion() throws ParseException {
        negocioNuevo("BASICO", Patrones.VENTA_DIRECTA);

        ResultadoDeLogin entrada = autenticacion.entrar(
                new CredencialesDeAcceso(correo, CLAVE, null, null, null));

        assertThat(SignedJWT.parse(entrada.tokenDeAcceso()).getJWTClaimsSet()
                .getStringListClaim("modulos"))
                .contains("VENTAS", "INVENTARIO")
                .doesNotContain("FACTURACION", "CAJA", "MULTISUCURSAL");
    }

    @Test
    @DisplayName("Criterio 3: cambiar de plan recalcula modulos, rota la suscripcion y avisa")
    void cambiarDePlanRecalculaYAvisa() {
        NegocioCreado negocio = negocioNuevo("BASICO", Patrones.VENTA_DIRECTA);

        ResumenDelNegocio despues = comoAdmin(negocio, () -> gestion.cambiarDePlan(
                new SolicitudDeCambioDePlan("PROFESIONAL", "El cliente crecio")));

        assertThat(despues.plan()).isEqualTo("PROFESIONAL");
        assertThat(despues.modulos()).extracting(ModuloActivo::codigo)
                .contains("FACTURACION", "CLIENTES", "COMPRAS", "REPORTES");
        assertThat(consultar("select estado from suscripciones where negocio_id = '"
                + negocio.negocioId() + "' order by fecha_inicio, creado_en"))
                .as("la anterior se cierra y la nueva queda vigente")
                .containsExactly("VENCIDA", "VIGENTE");
        assertThat(consultar("select tipo_evento from outbox_eventos where negocio_id = '"
                + negocio.negocioId() + "' order by creado_en"))
                .contains("plan_cambiado");
    }

    @Test
    @DisplayName("Bajar de plan apaga lo del plan, pero respeta lo que se pago aparte")
    void bajarDePlanRespetaElAddon() {
        NegocioCreado negocio = negocioNuevo("PROFESIONAL", Patrones.VENTA_DIRECTA);
        comoAdmin(negocio, () -> gestion.activarComoAddon("CAJA"));

        ResumenDelNegocio despues = comoAdmin(negocio, () -> gestion.cambiarDePlan(
                new SolicitudDeCambioDePlan("BASICO", "El cliente se achico")));

        assertThat(despues.modulos()).extracting(ModuloActivo::codigo)
                .as("CAJA se pago aparte: no se lo lleva un cambio de plan")
                .contains("CAJA")
                .doesNotContain("FACTURACION", "CLIENTES");
    }

    @Test
    @DisplayName("Un modulo del nucleo no se apaga")
    void elNucleoNoSeApaga() {
        NegocioCreado negocio = negocioNuevo("BASICO", Patrones.VENTA_DIRECTA);

        assertThatThrownBy(() -> comoAdmin(negocio, () -> {
            gestion.desactivar("USUARIOS");
            return null;
        })).isInstanceOf(ReglaDeNegocioException.class);
    }

    @Test
    @DisplayName("Tampoco se apaga uno del que otro depende")
    void noSeApagaLoQueOtroNecesita() {
        NegocioCreado negocio = negocioNuevo("PROFESIONAL", Patrones.VENTA_DIRECTA);

        assertThatThrownBy(() -> comoAdmin(negocio, () -> {
            gestion.desactivar("VENTAS");
            return null;
        })).isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("FACTURACION");
    }

    @Test
    @DisplayName("Un modulo de otro patron no se enciende ni pagandolo")
    void elModuloDeOtroPatronNoAplica() {
        NegocioCreado negocio = negocioNuevo("PROFESIONAL", Patrones.VENTA_DIRECTA);

        assertThatThrownBy(() -> comoAdmin(negocio, () -> gestion.activarComoAddon("MENU")))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("COMANDA");
    }

    @Test
    @DisplayName("HU-020: el resumen trae plan, limites y modulos")
    void elResumenTraeLoQueLaAppNecesita() {
        NegocioCreado negocio = negocioNuevo("PROFESIONAL", Patrones.VENTA_DIRECTA);

        ResumenDelNegocio resumen = comoAdmin(negocio, gestion::miNegocio);

        assertThat(resumen.plan()).isEqualTo("PROFESIONAL");
        assertThat(resumen.patronOperativo()).isEqualTo(Patrones.VENTA_DIRECTA);
        assertThat(resumen.maxUsuarios()).isEqualTo(10);
        assertThat(resumen.maxSucursales()).isEqualTo(1);
        assertThat(resumen.usuariosActivos()).isOne();
        assertThat(resumen.modulos()).isNotEmpty();
    }

    @Test
    @DisplayName("Sin el permiso de configuracion, no se toca ningun modulo")
    void sinPermisoNoSeTocanLosModulos() {
        NegocioCreado negocio = negocioNuevo("BASICO", Patrones.VENTA_DIRECTA);

        assertThatThrownBy(() -> enContexto(negocio.negocioId(), negocio.administradorId(),
                Set.of("CONFIGURACION_NEGOCIO_VER"), () -> gestion.activarComoAddon("CAJA")))
                .isInstanceOf(SinPermisoException.class);
    }

    @Test
    @DisplayName("Los modulos que devuelve el resumen son los mismos que trae el token")
    void elResumenYElTokenDicenLoMismo() throws ParseException {
        NegocioCreado negocio = negocioNuevo("PROFESIONAL", Patrones.COMANDA);
        ResultadoDeLogin entrada = autenticacion.entrar(
                new CredencialesDeAcceso(correo, CLAVE, null, null, null));

        List<String> enElToken = SignedJWT.parse(entrada.tokenDeAcceso()).getJWTClaimsSet()
                .getStringListClaim("modulos");
        List<String> enElResumen = comoAdmin(negocio, gestion::miNegocio).modulos().stream()
                .map(ModuloActivo::codigo)
                .toList();

        assertThat(enElToken).containsExactlyInAnyOrderElementsOf(enElResumen);
    }
}
