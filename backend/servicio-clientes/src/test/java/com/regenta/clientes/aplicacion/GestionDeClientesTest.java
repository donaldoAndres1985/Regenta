package com.regenta.clientes.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.regenta.clientes.BaseDeClientes;
import com.regenta.comun.errores.RecursoDuplicadoException;
import com.regenta.comun.errores.ReglaDeNegocioException;
import com.regenta.comun.errores.SinPermisoException;

/** HU-021. Crear y consultar clientes. */
class GestionDeClientesTest extends BaseDeClientes {

    private static final Set<String> DE_VENDEDOR = Set.of("CLIENTES_CLIENTE_VER",
            "CLIENTES_CLIENTE_CREAR", "CLIENTES_CLIENTE_EDITAR");

    @Autowired
    private GestionDeClientes clientes;

    private final UUID negocioA = UUID.randomUUID();
    private final UUID negocioB = UUID.randomUUID();
    private final UUID vendedor = UUID.randomUUID();

    private ClienteDelNegocio crearEn(UUID negocio, SolicitudDeCliente solicitud) {
        return enContexto(negocio, vendedor, DE_VENDEDOR, () -> clientes.crear(solicitud));
    }

    private static SolicitudDeCliente natural(String nombres, String tipoDoc, String numeroDoc) {
        return new SolicitudDeCliente("NATURAL", tipoDoc, numeroDoc, null, nombres, "Perez", null,
                "cliente@correo.co", "3001112233", null, "MINORISTA", null);
    }

    private static SolicitudDeCliente juridica(String razonSocial, String nit) {
        return new SolicitudDeCliente("JURIDICA", "NIT", nit, "7", null, null, razonSocial,
                "empresa@correo.co", "6041234567", null, "MAYORISTA", null);
    }

    @Test
    @DisplayName("Criterio 1: un documento ya registrado en mi negocio responde 409")
    void documentoDuplicadoEnElMismoNegocio() {
        crearEn(negocioA, natural("Ana", "CC", "52000111"));

        assertThatThrownBy(() -> crearEn(negocioA, natural("Otra Ana", "CC", "52000111")))
                .isInstanceOf(RecursoDuplicadoException.class);
    }

    @Test
    @DisplayName("El mismo documento en otro negocio si se permite: son universos aparte")
    void elMismoDocumentoEnOtroNegocioSiEntra() {
        crearEn(negocioA, natural("Ana", "CC", "52000111"));

        assertThatCode(() -> crearEn(negocioB, natural("Ana en B", "CC", "52000111")))
                .doesNotThrowAnyException();

        assertThat(comoElServicio(negocioA, "select nombre_display from clientes order by 1"))
                .containsExactly("Ana Perez");
        assertThat(comoElServicio(negocioB, "select nombre_display from clientes order by 1"))
                .containsExactly("Ana en B Perez");
    }

    @Test
    @DisplayName("Criterio 2: JURIDICA sin razon social responde 422 nombrando el campo")
    void juridicaSinRazonSocial() {
        assertThatThrownBy(() -> crearEn(negocioA, juridica(null, "900123456")))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("razon social");
    }

    @Test
    @DisplayName("Criterio 3: la busqueda por nombre parcial encuentra, aisla por negocio y no barre la tabla")
    void busquedaPorNombreParcial() {
        crearEn(negocioA, natural("Ferreteria La Esquina", "NIT", "900111111"));
        crearEn(negocioA, natural("Distribuidora El Roble", "NIT", "900222222"));
        crearEn(negocioB, natural("Ferreteria de Otro Negocio", "NIT", "900333333"));

        List<ClienteDelNegocio> hallados = enContexto(negocioA, vendedor, DE_VENDEDOR,
                () -> clientes.buscar("ferre"));
        assertThat(hallados).extracting(ClienteDelNegocio::nombreDisplay)
                .containsExactly("Ferreteria La Esquina Perez");

        // Menos de tres caracteres no llega al backend.
        assertThatThrownBy(() -> enContexto(negocioA, vendedor, DE_VENDEDOR,
                () -> clientes.buscar("fe")))
                .isInstanceOf(ReglaDeNegocioException.class);

        // Con volumen y el scan secuencial penalizado, la busqueda parcial se
        // resuelve por indice acotado al negocio: nunca cae en un Seq Scan de
        // toda la tabla.
        for (int i = 0; i < 40; i++) {
            crearEn(negocioA, natural("Relleno " + i, "CC", "9" + (1000000 + i)));
        }
        String plan = planDe(negocioA, "select id from clientes where eliminado_en is null"
                + " and nombre_display ilike '%ferre%'");
        assertThat(plan).doesNotContain("Seq Scan").contains("Index");

        // Y el indice trigram sobre el nombre existe (lo usa el planificador en
        // cuanto textlike/texticlike quedan marcados LEAKPROOF; ver V4).
        assertThat(consultar(
                "select indexdef from pg_indexes where indexname = 'ix_clientes_busqueda'"))
                .singleElement(org.assertj.core.api.InstanceOfAssertFactories.STRING)
                .contains("gin").contains("gin_trgm_ops");
    }

    @Test
    @DisplayName("Criterio 4: un cliente sin documento («consumidor final») se permite y se repite")
    void consumidorFinalSinDocumento() {
        SolicitudDeCliente sinDoc = new SolicitudDeCliente("NATURAL", "SIN_IDENTIFICAR", null, null,
                null, null, null, null, null, null, null, null);

        ClienteDelNegocio uno = crearEn(negocioA, sinDoc);
        ClienteDelNegocio otro = crearEn(negocioA, sinDoc);

        assertThat(uno.id()).isNotEqualTo(otro.id());
        assertThat(uno.nombreDisplay()).isEqualTo("Consumidor final");
        assertThat(comoElServicio(negocioA, "select count(*) from clientes"
                + " where tipo_documento = 'SIN_IDENTIFICAR'")).containsExactly("2");
    }

    @Test
    @DisplayName("Sin el permiso de crear, la operacion se rechaza con 403")
    void sinPermisoNoCrea() {
        assertThatThrownBy(() -> enContexto(negocioA, vendedor, Set.of("CLIENTES_CLIENTE_VER"),
                () -> clientes.crear(natural("Ana", "CC", "52000111"))))
                .isInstanceOf(SinPermisoException.class);
    }

    @Test
    @DisplayName("El alta deja un evento cliente_creado en el outbox del negocio")
    void publicaClienteCreado() {
        ClienteDelNegocio ana = crearEn(negocioA, natural("Ana", "CC", "52000111"));

        // outbox_eventos no lleva RLS: se filtra por agregado_id, no por negocio,
        // porque otras pruebas de la suite tambien dejan cliente_creado ahi.
        assertThat(comoElServicio(negocioA, "select count(*) from outbox_eventos"
                + " where tipo_evento = 'cliente_creado' and agregado_id = '" + ana.id() + "'"))
                .containsExactly("1");
    }
}
