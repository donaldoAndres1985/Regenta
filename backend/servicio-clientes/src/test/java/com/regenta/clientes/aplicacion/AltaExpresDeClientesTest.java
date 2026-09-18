package com.regenta.clientes.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.regenta.clientes.BaseDeClientes;
import com.regenta.comun.errores.RecursoDuplicadoException;
import com.regenta.comun.errores.ReglaDeNegocioException;
import com.regenta.comun.errores.SinPermisoException;

/**
 * HU-114. Crear el cliente en el mostrador, con lo mínimo para facturar, sin
 * perder el carrito: tipo y número de documento, nombre o razón social, y
 * correo. El resto de la ficha se completa después.
 */
class AltaExpresDeClientesTest extends BaseDeClientes {

    private static final Set<String> VENDEDOR =
            Set.of("CLIENTES_CLIENTE_VER", "CLIENTES_CLIENTE_CREAR");
    private static final Set<String> SOLO_MIRA = Set.of("CLIENTES_CLIENTE_VER");

    @Autowired
    private AltaExpresDeClientes alta;

    private final UUID negocioA = UUID.randomUUID();
    private final UUID negocioB = UUID.randomUUID();
    private final UUID vendedor = UUID.randomUUID();

    private static SolicitudExpres ferreteria(String numero) {
        return new SolicitudExpres(null, "JURIDICA", "NIT", numero, null, null, null,
                "Ferretería El Tornillo SAS", "facturas@tornillo.co");
    }

    @Test
    @DisplayName("Criterio 1: con documento, nombre y correo, el cliente queda creado")
    void seCreaConLoMinimoParaFacturar() {
        ClienteDelNegocio creado = enContexto(negocioA, vendedor, VENDEDOR,
                () -> alta.crear(ferreteria("900123456")));

        assertThat(creado.id()).isNotNull();
        assertThat(creado.nombreDisplay()).isEqualTo("Ferretería El Tornillo SAS");
        assertThat(creado.email()).isEqualTo("facturas@tornillo.co");
        assertThat(contar("select count(*) from clientes where id = '" + creado.id() + "'"))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("Sin correo no se crea: es donde llega la factura electrónica")
    void sinCorreoNoSeCrea() {
        assertThatThrownBy(() -> enContexto(negocioA, vendedor, VENDEDOR,
                () -> alta.crear(new SolicitudExpres(null, "JURIDICA", "NIT", "900999888", null,
                        null, null, "Sin Correo SAS", null))))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("correo");
    }

    @Test
    @DisplayName("Sin nombre ni razón social tampoco: la factura sale a nombre de alguien")
    void sinNombreNoSeCrea() {
        assertThatThrownBy(() -> enContexto(negocioA, vendedor, VENDEDOR,
                () -> alta.crear(new SolicitudExpres(null, "NATURAL", "CC", "1020304050", null,
                        null, null, null, "alguien@correo.co"))))
                .isInstanceOf(ReglaDeNegocioException.class);
    }

    @Test
    @DisplayName("Criterio 2: el documento repetido responde 409 y dice cuál es el que ya existe")
    void elDocumentoRepetidoOfreceElQueYaExiste() {
        ClienteDelNegocio primero = enContexto(negocioA, vendedor, VENDEDOR,
                () -> alta.crear(ferreteria("900123456")));

        RecursoDuplicadoException choque = catchThrowableOfType(
                () -> enContexto(negocioA, vendedor, VENDEDOR, () -> alta.crear(ferreteria("900123456"))),
                RecursoDuplicadoException.class);

        assertThat(choque).isNotNull();
        assertThat(choque.getDatos())
                .as("sin el id, la pantalla no puede ofrecer asignar el que ya existe")
                .containsEntry("cliente_id", primero.id().toString());
    }

    @Test
    @DisplayName("Criterio 3: el dígito de verificación del NIT se calcula solo")
    void elDigitoDeVerificacionSeCalcula() {
        ClienteDelNegocio creado = enContexto(negocioA, vendedor, VENDEDOR,
                () -> alta.crear(ferreteria("900123456")));

        assertThat(creado.digitoVerificacion()).isEqualTo("8");
    }

    @Test
    @DisplayName("Criterio 3: el dígito escrito a mano se respeta, no se pisa con el calculado")
    void elDigitoEscritoAManoSeRespeta() {
        ClienteDelNegocio creado = enContexto(negocioA, vendedor, VENDEDOR,
                () -> alta.crear(new SolicitudExpres(null, "JURIDICA", "NIT", "900123456", "9",
                        null, null, "Ferretería El Tornillo SAS", "facturas@tornillo.co")));

        assertThat(creado.digitoVerificacion())
                .as("la cámara de comercio manda sobre la fórmula")
                .isEqualTo("9");
    }

    @Test
    @DisplayName("Una cédula no lleva dígito de verificación")
    void laCedulaNoLlevaDigito() {
        ClienteDelNegocio creado = enContexto(negocioA, vendedor, VENDEDOR,
                () -> alta.crear(new SolicitudExpres(null, "NATURAL", "CC", "1020304050", null,
                        "Ana", "Restrepo", null, "ana@correo.co")));

        assertThat(creado.digitoVerificacion()).isNull();
        assertThat(creado.nombreDisplay()).contains("Ana");
    }

    @Test
    @DisplayName("Criterio 4: sin el permiso de crear, no se crea")
    void sinPermisoNoSeCrea() {
        assertThatThrownBy(() -> enContexto(negocioA, vendedor, SOLO_MIRA,
                () -> alta.crear(ferreteria("900555444"))))
                .isInstanceOf(SinPermisoException.class);
    }

    @Test
    @DisplayName("Criterio 5: el cliente creado sin conexión sube con su propio id, y subirlo dos veces no lo duplica")
    void elCreadoSinConexionSubeConSuId() {
        UUID idDelDispositivo = UUID.randomUUID();
        SolicitudExpres offline = new SolicitudExpres(idDelDispositivo, "JURIDICA", "NIT",
                "901222333", null, null, null, "Creado sin señal SAS", "offline@correo.co");

        ClienteDelNegocio creado = enContexto(negocioA, vendedor, VENDEDOR, () -> alta.crear(offline));
        ClienteDelNegocio reintento = enContexto(negocioA, vendedor, VENDEDOR,
                () -> alta.crear(offline));

        assertThat(creado.id()).isEqualTo(idDelDispositivo);
        assertThat(reintento.id())
                .as("la cola de sincronización reintenta: el segundo intento devuelve el mismo, no un 409")
                .isEqualTo(idDelDispositivo);
        assertThat(contar("select count(*) from clientes where id = '" + idDelDispositivo + "'"))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("El mismo documento en otro negocio no choca: son clientes distintos")
    void elMismoDocumentoEnOtroNegocioNoChoca() {
        enContexto(negocioA, vendedor, VENDEDOR, () -> alta.crear(ferreteria("900777666")));

        ClienteDelNegocio delB = enContexto(negocioB, vendedor, VENDEDOR,
                () -> alta.crear(ferreteria("900777666")));

        assertThat(delB.id()).isNotNull();
        assertThat(contar("select count(*) from clientes where numero_documento = '900777666'"))
                .isEqualTo(2);
    }
}
