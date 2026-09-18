package com.regenta.usuarios.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.regenta.comun.errores.NoEncontradoException;
import com.regenta.comun.errores.RecursoDuplicadoException;
import com.regenta.comun.errores.ReglaDeNegocioException;
import com.regenta.usuarios.BaseDeUsuarios;
import com.regenta.usuarios.domain.Patrones;

/**
 * HU-011 (alta del negocio) y HU-012 (su primer administrador).
 *
 * <p>Las comprobaciones se hacen contra la base, no contra lo que devuelve el
 * servicio: lo que importa es que el negocio quede realmente listo para operar.
 */
class AltaDeNegociosTest extends BaseDeUsuarios {

    private static final AtomicInteger CONSECUTIVO = new AtomicInteger(1000);

    @Autowired
    private AltaDeNegocios alta;

    private static SolicitudDeAlta solicitud(String documento, String patron, String plan) {
        return new SolicitudDeAlta("Panaderia La Espiga", "La Espiga SAS", "NIT", documento, "7",
                patron, plan, "CO", "America/Bogota", "COP", "es-CO",
                new SolicitudDeAlta.Administrador("dueno" + documento + "@espiga.co", "Ana",
                        "Restrepo", "clave-de-prueba-larga"));
    }

    private static String documentoNuevo() {
        return "9001234" + CONSECUTIVO.incrementAndGet();
    }

    @Test
    @DisplayName("Criterio 2: quedan el negocio, su configuracion y una suscripcion vigente")
    void elAltaDejaElNegocioListoParaOperar() {
        NegocioCreado creado = alta.registrar(
                solicitud(documentoNuevo(), Patrones.VENTA_DIRECTA, "BASICO"));
        String id = creado.negocioId().toString();

        assertThat(contar("select count(*) from negocios where id = '" + id + "'")).isOne();
        assertThat(contar("select count(*) from configuracion_negocio where negocio_id = '" + id + "'"))
                .isOne();
        assertThat(consultar("select estado from suscripciones where negocio_id = '" + id
                + "' and fecha_fin is null")).containsExactly("VIGENTE");
        assertThat(consultar("select estado from negocios where id = '" + id + "'"))
                .containsExactly("TRIAL");
        assertThat(creado.plan()).isEqualTo("BASICO");
    }

    @Test
    @DisplayName("Criterio 1: el mismo documento fiscal en el mismo pais responde 409")
    void elMismoDocumentoNoSeDuplica() {
        String documento = documentoNuevo();
        alta.registrar(solicitud(documento, Patrones.VENTA_DIRECTA, "BASICO"));

        assertThatThrownBy(() -> alta.registrar(
                solicitud(documento, Patrones.COMANDA, "PROFESIONAL")))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessageContaining(documento);

        assertThat(contar("select count(*) from negocios where numero_documento = '" + documento + "'"))
                .isOne();
    }

    @Test
    @DisplayName("Criterio 3: se activan los modulos del plan que aplican al patron")
    void losModulosSonLosDelPlanQueAplicanAlPatron() {
        NegocioCreado creado = alta.registrar(
                solicitud(documentoNuevo(), Patrones.VENTA_DIRECTA, "BASICO"));

        List<String> activos = consultar("select modulo_codigo from negocio_modulos where negocio_id = '"
                + creado.negocioId() + "' order by modulo_codigo");

        assertThat(activos).containsExactly("CONFIGURACION", "INVENTARIO", "USUARIOS", "VENTAS");
        assertThat(activos)
                .as("un negocio de venta directa no activa lo de los otros dos patrones")
                .doesNotContain("MENU", "COMANDAS", "MESAS", "RECURSOS", "RESERVAS");
        assertThat(creado.modulosActivos()).containsExactlyInAnyOrderElementsOf(activos);
    }

    @Test
    @DisplayName("Criterio 3: el mismo plan en otro patron activa otros modulos")
    void elPatronDeComandaActivaLoSuyo() {
        NegocioCreado creado = alta.registrar(
                solicitud(documentoNuevo(), Patrones.COMANDA, "BASICO"));

        assertThat(consultar("select modulo_codigo from negocio_modulos where negocio_id = '"
                + creado.negocioId() + "' order by modulo_codigo"))
                .containsExactly("COMANDAS", "CONFIGURACION", "MENU", "MESAS", "USUARIOS");
    }

    @Test
    @DisplayName("Criterio 4: sale el evento negocio_creado con id, plan y patron")
    void seRegistraElEventoDelAlta() {
        NegocioCreado creado = alta.registrar(
                solicitud(documentoNuevo(), Patrones.RESERVA, "PROFESIONAL"));

        List<String> eventos = consultar("select tipo_evento from outbox_eventos where negocio_id = '"
                + creado.negocioId() + "'");
        assertThat(eventos).containsExactly("negocio_creado");

        String payload = consultar("select payload from outbox_eventos where negocio_id = '"
                + creado.negocioId() + "'").get(0);
        assertThat(payload)
                // HU-097 criterio 3: servicio-reportes necesita la zona horaria del
                // negocio para cortar los agregados diarios a su medianoche, no a la
                // del servidor. La toma de este mismo evento, no consulta esta base.
                .contains("America/Bogota")
                .contains(creado.negocioId().toString())
                .contains("PROFESIONAL")
                .contains("RESERVA");
    }

    @Test
    @DisplayName("HU-115: negocio_creado lleva la identidad fiscal, que es lo que va en la factura")
    void elEventoLlevaLaIdentidadFiscal() {
        String documento = documentoNuevo();

        NegocioCreado creado = alta.registrar(
                solicitud(documento, Patrones.VENTA_DIRECTA, "PROFESIONAL"));

        String payload = consultar("select payload from outbox_eventos where negocio_id = '"
                + creado.negocioId() + "'").get(0);
        assertThat(payload)
                .as("Facturacion no puede consultar esta base: si no viaja aqui, "
                        + "la factura sale sin emisor")
                .contains("\"numero_documento\": \"" + documento + "\"")
                .contains("\"tipo_documento\"")
                .contains("\"razon_social\"");
    }

    @Test
    @DisplayName("HU-012 criterio 1: queda un administrador con todos los permisos")
    void elPrimerUsuarioEsAdministradorConTodo() {
        NegocioCreado creado = alta.registrar(
                solicitud(documentoNuevo(), Patrones.VENTA_DIRECTA, "EMPRESARIAL"));
        String id = creado.negocioId().toString();

        assertThat(consultar("select estado from usuarios where id = '" + creado.administradorId() + "'"))
                .containsExactly("ACTIVO");
        assertThat(contar("select count(*) from usuario_roles ur join roles r on r.id = ur.rol_id"
                + " where r.negocio_id = '" + id + "' and r.es_sistema")).isOne();

        long delAdministrador = contar("select count(*) from rol_permisos rp join roles r"
                + " on r.id = rp.rol_id where r.negocio_id = '" + id + "' and r.es_sistema");
        long enElCatalogo = contar("select count(*) from permisos");
        assertThat(delAdministrador).isEqualTo(enElCatalogo);
    }

    @Test
    @DisplayName("HU-012 criterios 2 y 3: plantillas comunes mas las del patron, y solo esas")
    void seInstancianLasPlantillasDelPatron() {
        NegocioCreado creado = alta.registrar(
                solicitud(documentoNuevo(), Patrones.VENTA_DIRECTA, "PROFESIONAL"));

        List<String> roles = consultar("select nombre from roles where negocio_id = '"
                + creado.negocioId() + "' order by nombre");

        assertThat(roles).contains("Administrador", "Gerente", "Vendedor", "Cajero");
        assertThat(roles).doesNotContain("Recepcionista", "Mesero", "Cocinero");
    }

    @Test
    @DisplayName("HU-019 criterio 2: el negocio nace con su sucursal principal")
    void naceConSucursalPrincipal() {
        NegocioCreado creado = alta.registrar(
                solicitud(documentoNuevo(), Patrones.VENTA_DIRECTA, "BASICO"));

        assertThat(consultar("select codigo from sucursales where negocio_id = '"
                + creado.negocioId() + "' and es_principal")).containsExactly("001");
        assertThat(creado.sucursalPrincipalId()).isNotNull();
    }

    @Test
    @DisplayName("El correo del administrador queda en el directorio con el que empieza el login")
    void elCorreoQuedaEnElDirectorio() {
        NegocioCreado creado = alta.registrar(
                solicitud(documentoNuevo(), Patrones.VENTA_DIRECTA, "BASICO"));

        assertThat(consultar("select negocio_id from acceso_por_correo where email = '"
                + creado.administradorEmail() + "'"))
                .containsExactly(creado.negocioId().toString());
    }

    @Test
    @DisplayName("Un patron que no existe no se da de alta")
    void elPatronTieneQueSerUnoDeLosTres() {
        assertThatThrownBy(() -> alta.registrar(
                solicitud(documentoNuevo(), "FRANQUICIA", "BASICO")))
                .isInstanceOf(ReglaDeNegocioException.class);
    }

    @Test
    @DisplayName("Un plan que no existe tampoco")
    void elPlanTieneQueExistir() {
        assertThatThrownBy(() -> alta.registrar(
                solicitud(documentoNuevo(), Patrones.VENTA_DIRECTA, "PLATINO")))
                .isInstanceOf(NoEncontradoException.class);
    }

    @Test
    @DisplayName("La RLS corta de verdad: sin negocio fijado no se ve una sola fila")
    void elAislamientoCortaDeVerdad() {
        NegocioCreado uno = alta.registrar(
                solicitud(documentoNuevo(), Patrones.VENTA_DIRECTA, "BASICO"));
        NegocioCreado otro = alta.registrar(
                solicitud(documentoNuevo(), Patrones.COMANDA, "BASICO"));

        String cuentaUsuarios = "select count(*) from usuarios";
        assertThat(comoElServicio(null, cuentaUsuarios))
                .as("sin app.negocio_id fijado, cero filas: falla cerrado")
                .containsExactly("0");
        assertThat(comoElServicio(uno.negocioId(), cuentaUsuarios)).containsExactly("1");
        assertThat(comoElServicio(otro.negocioId(), cuentaUsuarios)).containsExactly("1");
        assertThat(comoElServicio(UUID.randomUUID(), cuentaUsuarios)).containsExactly("0");

        assertThat(comoElServicio(uno.negocioId(), "select count(*) from negocios"))
                .as("la tabla del propio negocio se filtra por su id")
                .containsExactly("1");
    }
}
