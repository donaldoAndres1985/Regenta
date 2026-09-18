package com.regenta.usuarios.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.text.ParseException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.nimbusds.jwt.SignedJWT;
import com.regenta.comun.errores.CuentaBloqueadaException;
import com.regenta.comun.errores.NoAutenticadoException;
import com.regenta.usuarios.BaseDeUsuarios;
import com.regenta.usuarios.domain.Patrones;
import com.regenta.usuarios.domain.Usuario;

/**
 * HU-013 (entrar) y HU-014 (mantenerse dentro).
 *
 * <p>El token se abre y se miran sus claims: son el contrato con el gateway y
 * con los quince servicios, no un detalle interno.
 */
class AutenticacionTest extends BaseDeUsuarios {

    private static final AtomicInteger CONSECUTIVO = new AtomicInteger(2000);
    private static final String CLAVE = "clave-de-prueba-larga";

    @Autowired
    private AltaDeNegocios alta;

    @Autowired
    private Autenticacion autenticacion;

    private NegocioCreado negocioNuevo(String correo, String patron) {
        String documento = "7001234" + CONSECUTIVO.incrementAndGet();
        return alta.registrar(new SolicitudDeAlta("Tienda " + documento, null, "NIT", documento,
                "1", patron, "PROFESIONAL", "CO", "America/Bogota", "COP", "es-CO",
                new SolicitudDeAlta.Administrador(correo, "Ana", "Restrepo", CLAVE)));
    }

    private static String correoNuevo() {
        return "ana" + CONSECUTIVO.incrementAndGet() + "@regenta.co";
    }

    @Test
    @DisplayName("Criterio 1: el token trae negocio, plan, patron, roles y sucursales")
    void elTokenLlevaTodoLoQueElGatewayNecesita() throws ParseException {
        String correo = correoNuevo();
        NegocioCreado negocio = negocioNuevo(correo, Patrones.VENTA_DIRECTA);

        ResultadoDeLogin entrada = autenticacion.entrar(
                new CredencialesDeAcceso(correo, CLAVE, null, "celular-de-ana", "ANDROID"));

        assertThat(entrada.debeElegirNegocio()).isFalse();
        assertThat(entrada.tokenDeRefresco()).isNotBlank();
        assertThat(entrada.expiraEnSegundos()).isPositive();

        SignedJWT token = SignedJWT.parse(entrada.tokenDeAcceso());
        var claims = token.getJWTClaimsSet();
        assertThat(claims.getStringClaim("negocio_id")).isEqualTo(negocio.negocioId().toString());
        assertThat(claims.getStringClaim("plan")).isEqualTo("PROFESIONAL");
        assertThat(claims.getStringClaim("patron")).isEqualTo(Patrones.VENTA_DIRECTA);
        assertThat(claims.getStringClaim("estado_negocio")).isEqualTo("TRIAL");
        assertThat(claims.getStringListClaim("roles")).contains("ADMINISTRADOR");
        assertThat(claims.getStringListClaim("modulos")).contains("VENTAS", "FACTURACION");
        assertThat(claims.getStringListClaim("permisos")).contains("VENTAS_VENTA_CREAR");
        assertThat(claims.getSubject()).isEqualTo(negocio.administradorId().toString());
        assertThat(claims.getExpirationTime()).isAfter(claims.getIssueTime());
    }

    @Test
    @DisplayName("Criterio 2: el mismo correo en dos negocios devuelve la lista para elegir")
    void conDosNegociosPideElegir() {
        String correo = correoNuevo();
        NegocioCreado uno = negocioNuevo(correo, Patrones.VENTA_DIRECTA);
        NegocioCreado otro = negocioNuevo(correo, Patrones.COMANDA);

        ResultadoDeLogin sinElegir = autenticacion.entrar(
                new CredencialesDeAcceso(correo, CLAVE, null, null, null));

        assertThat(sinElegir.debeElegirNegocio()).isTrue();
        assertThat(sinElegir.tokenDeAcceso()).isNull();
        assertThat(sinElegir.negocios()).extracting(NegocioParaElegir::negocioId)
                .containsExactlyInAnyOrder(uno.negocioId(), otro.negocioId());

        ResultadoDeLogin eligiendo = autenticacion.entrar(
                new CredencialesDeAcceso(correo, CLAVE, otro.negocioId(), null, null));

        assertThat(eligiendo.debeElegirNegocio()).isFalse();
        assertThat(eligiendo.negocioId()).isEqualTo(otro.negocioId());
        assertThat(eligiendo.patron()).isEqualTo(Patrones.COMANDA);
    }

    @Test
    @DisplayName("Criterio 3: a la quinta seguida la cuenta se bloquea y responde 423")
    void aLaQuintaSeBloquea() {
        String correo = correoNuevo();
        negocioNuevo(correo, Patrones.VENTA_DIRECTA);
        CredencialesDeAcceso malas = new CredencialesDeAcceso(correo, "no-es-la-clave", null,
                null, null);

        for (int intento = 1; intento < Usuario.INTENTOS_ANTES_DE_BLOQUEAR; intento++) {
            assertThatThrownBy(() -> autenticacion.entrar(malas))
                    .isInstanceOf(NoAutenticadoException.class);
        }
        assertThatThrownBy(() -> autenticacion.entrar(malas))
                .isInstanceOf(CuentaBloqueadaException.class);

        assertThatThrownBy(() -> autenticacion.entrar(
                new CredencialesDeAcceso(correo, CLAVE, null, null, null)))
                .as("ya bloqueada, ni con la clave buena")
                .isInstanceOf(CuentaBloqueadaException.class);
    }

    @Test
    @DisplayName("HU-101: una clave incorrecta publica login_fallido para la bitácora de auditoría")
    void publicaLoginFallido() {
        String correo = correoNuevo();
        NegocioCreado negocio = negocioNuevo(correo, Patrones.VENTA_DIRECTA);
        CredencialesDeAcceso malas = new CredencialesDeAcceso(correo, "no-es-la-clave", null, null, null);

        assertThatThrownBy(() -> autenticacion.entrar(malas)).isInstanceOf(NoAutenticadoException.class);

        List<String> eventos = consultar("select tipo_evento from outbox_eventos where negocio_id = '"
                + negocio.negocioId() + "' and tipo_evento = 'login_fallido'");
        assertThat(eventos).hasSize(1);
        assertThat(consultar("select payload from outbox_eventos where negocio_id = '" + negocio.negocioId()
                + "' and tipo_evento = 'login_fallido'").get(0)).contains(correo);
    }

    @Test
    @DisplayName("Criterio 4: un usuario INACTIVO no entra, y no se le dice por que")
    void elInactivoNoEntraYNoSeLeDicePorQue() {
        String correo = correoNuevo();
        NegocioCreado negocio = negocioNuevo(correo, Patrones.VENTA_DIRECTA);
        comoSuperusuario("UPDATE core_identidad.usuarios SET estado = 'INACTIVO' WHERE id = '"
                + negocio.administradorId() + "'");

        assertThatThrownBy(() -> autenticacion.entrar(
                new CredencialesDeAcceso(correo, CLAVE, null, null, null)))
                .isInstanceOf(NoAutenticadoException.class)
                .hasMessageContaining("Correo o contraseña incorrectos");
    }

    @Test
    @DisplayName("Un correo que no existe responde igual que una clave mala")
    void elCorreoDesconocidoRespondeIgual() {
        assertThatThrownBy(() -> autenticacion.entrar(
                new CredencialesDeAcceso("nadie@regenta.co", CLAVE, null, null, null)))
                .isInstanceOf(NoAutenticadoException.class)
                .hasMessageContaining("Correo o contraseña incorrectos");
    }

    @Test
    @DisplayName("HU-014 criterio 1: refrescar rota el token y revoca el anterior")
    void refrescarRota() {
        String correo = correoNuevo();
        negocioNuevo(correo, Patrones.VENTA_DIRECTA);
        ResultadoDeLogin entrada = autenticacion.entrar(
                new CredencialesDeAcceso(correo, CLAVE, null, "celular-de-ana", "ANDROID"));

        ResultadoDeLogin refrescada = autenticacion.refrescar(
                new SolicitudDeRefresco(entrada.tokenDeRefresco(), "celular-de-ana", "ANDROID"));

        assertThat(refrescada.tokenDeRefresco()).isNotEqualTo(entrada.tokenDeRefresco());
        assertThat(refrescada.tokenDeAcceso()).isNotBlank();
        assertThat(contar("select count(*) from refresh_tokens where usuario_id = '"
                + entrada.usuarioId() + "' and revocado_en is not null"))
                .as("el anterior queda revocado y apuntando al nuevo")
                .isOne();
        assertThat(contar("select count(*) from refresh_tokens where usuario_id = '"
                + entrada.usuarioId() + "' and reemplazado_por is not null")).isOne();
    }

    @Test
    @DisplayName("HU-014 criterio 2: reusar uno ya usado tumba la sesion entera")
    void elReusoTumbaLaSesion() {
        String correo = correoNuevo();
        negocioNuevo(correo, Patrones.VENTA_DIRECTA);
        ResultadoDeLogin entrada = autenticacion.entrar(
                new CredencialesDeAcceso(correo, CLAVE, null, "celular-de-ana", "ANDROID"));
        ResultadoDeLogin segunda = autenticacion.refrescar(
                new SolicitudDeRefresco(entrada.tokenDeRefresco(), "celular-de-ana", "ANDROID"));

        assertThatThrownBy(() -> autenticacion.refrescar(
                new SolicitudDeRefresco(entrada.tokenDeRefresco(), "celular-de-ana", "ANDROID")))
                .isInstanceOf(NoAutenticadoException.class);

        assertThatThrownBy(() -> autenticacion.refrescar(
                new SolicitudDeRefresco(segunda.tokenDeRefresco(), "celular-de-ana", "ANDROID")))
                .as("el que estaba vivo tambien cae: no se sabe cual mano es la del dueño")
                .isInstanceOf(NoAutenticadoException.class);

        assertThat(contar("select count(*) from refresh_tokens where usuario_id = '"
                + entrada.usuarioId() + "' and revocado_en is null")).isZero();
    }

    @Test
    @DisplayName("HU-014 criterio 3: revocar un dispositivo corta su acceso de inmediato")
    void revocarUnDispositivoCorta() {
        String correo = correoNuevo();
        NegocioCreado negocio = negocioNuevo(correo, Patrones.VENTA_DIRECTA);
        ResultadoDeLogin celular = autenticacion.entrar(
                new CredencialesDeAcceso(correo, CLAVE, null, "celular-de-ana", "ANDROID"));
        ResultadoDeLogin navegador = autenticacion.entrar(
                new CredencialesDeAcceso(correo, CLAVE, null, "portatil-de-ana", "WEB"));

        List<SesionActiva> abiertas = enSesionDe(negocio, autenticacion::sesiones);
        assertThat(abiertas).extracting(SesionActiva::dispositivoId)
                .containsExactlyInAnyOrder("celular-de-ana", "portatil-de-ana");

        enSesionDe(negocio, () -> autenticacion.cerrarSesionesDe("celular-de-ana"));

        assertThatThrownBy(() -> autenticacion.refrescar(
                new SolicitudDeRefresco(celular.tokenDeRefresco(), "celular-de-ana", "ANDROID")))
                .isInstanceOf(NoAutenticadoException.class);

        ResultadoDeLogin sigueViva = autenticacion.refrescar(
                new SolicitudDeRefresco(navegador.tokenDeRefresco(), "portatil-de-ana", "WEB"));
        assertThat(sigueViva.tokenDeAcceso())
                .as("el otro dispositivo no tiene por que enterarse")
                .isNotBlank();
    }

    /** Lo que hara el gateway: poner el negocio y el usuario en el contexto. */
    private <T> T enSesionDe(NegocioCreado negocio, java.util.function.Supplier<T> tarea) {
        java.util.concurrent.atomic.AtomicReference<T> resultado =
                new java.util.concurrent.atomic.AtomicReference<>();
        com.regenta.comun.negocio.ContextoDeNegocio.en(
                new com.regenta.comun.negocio.DatosDelNegocio(negocio.negocioId(),
                        negocio.administradorId(), negocio.plan(), negocio.patronOperativo(),
                        java.util.Set.of("ADMINISTRADOR"), java.util.Set.of(), java.util.Set.of(),
                        java.util.Set.of()),
                () -> resultado.set(tarea.get()));
        return resultado.get();
    }
}
