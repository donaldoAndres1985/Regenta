package com.regenta.clientes.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.regenta.clientes.BaseDeClientes;
import com.regenta.comun.errores.NoEncontradoException;

/**
 * HU-133. {@code crm.clientes} tiene {@code regimen_fiscal} y
 * {@code responsabilidades_fiscales} desde V1, y nadie los leía: la factura
 * salía sin un dato que sí teníamos.
 */
class RegimenFiscalDelClienteTest extends BaseDeClientes {

    private static final Set<String> VENDEDOR = Set.of("CLIENTES_CLIENTE_VER",
            "CLIENTES_CLIENTE_CREAR", "CLIENTES_CLIENTE_EDITAR");

    @Autowired
    private AltaExpresDeClientes alta;
    @Autowired
    private GestionDeClientes gestion;

    private final UUID negocioA = UUID.randomUUID();
    private final UUID negocioB = UUID.randomUUID();
    private final UUID vendedor = UUID.randomUUID();

    private static SolicitudExpres juridica(String numero, String regimen,
            List<String> responsabilidades) {
        return new SolicitudExpres(null, "JURIDICA", "NIT", numero, null, null, null,
                "Materiales Cruz S.A.S.", "compras@materialescruz.co", "6044482210", regimen,
                responsabilidades);
    }

    @Test
    @DisplayName("Criterio 1: al consultar un cliente vienen su régimen y sus responsabilidades fiscales")
    void laConsultaTraeElRegimen() {
        ClienteDelNegocio creado = enContexto(negocioA, vendedor, VENDEDOR, () -> alta.crear(
                juridica("900412883", "RESPONSABLE_IVA", List.of("O-13", "O-15"))));

        ClienteDelNegocio leido = enContexto(negocioA, vendedor, VENDEDOR,
                () -> gestion.ver(creado.id()));

        assertThat(leido.regimenFiscal()).isEqualTo("RESPONSABLE_IVA");
        assertThat(leido.responsabilidadesFiscales()).containsExactly("O-13", "O-15");
        assertThatThrownBy(() -> enContexto(negocioB, vendedor, VENDEDOR,
                () -> gestion.ver(creado.id())))
                .as("el régimen de un cliente no se ve desde otro negocio")
                .isInstanceOf(NoEncontradoException.class);
    }

    @Test
    @DisplayName("Criterio 2: en el alta exprés de una persona jurídica se indica el régimen y queda guardado")
    void elAltaExpresGuardaElRegimen() {
        ClienteDelNegocio creado = enContexto(negocioA, vendedor, VENDEDOR,
                () -> alta.crear(juridica("900412883", "NO_RESPONSABLE", null)));

        assertThat(consultar("select regimen_fiscal from clientes where id = '" + creado.id() + "'"))
                .containsExactly("NO_RESPONSABLE");
        assertThat(contar("select count(*) from clientes where negocio_id = '" + negocioB
                + "' and regimen_fiscal is not null")).isZero();
    }

    @Test
    @DisplayName("Sin régimen el cliente se crea igual: el valor por defecto lo pone la factura")
    void sinRegimenSeCreaIgual() {
        ClienteDelNegocio creado = enContexto(negocioA, vendedor, VENDEDOR,
                () -> alta.crear(juridica("900412884", null, null)));

        assertThat(creado.regimenFiscal()).isNull();
        assertThat(creado.responsabilidadesFiscales()).isEmpty();
    }

    @Test
    @DisplayName("Editar la ficha sin mandar el régimen no lo borra")
    void editarSinRegimenLoConserva() {
        ClienteDelNegocio creado = enContexto(negocioA, vendedor, VENDEDOR, () -> alta.crear(
                juridica("900412885", "RESPONSABLE_IVA", List.of("O-13"))));

        ClienteDelNegocio editado = enContexto(negocioA, vendedor, VENDEDOR,
                () -> gestion.actualizar(creado.id(), new SolicitudDeCliente(null, null, null, null,
                        null, null, "Materiales Cruz S.A.S.", "nuevo@materialescruz.co", null, null,
                        null, null)));

        assertThat(editado.regimenFiscal()).isEqualTo("RESPONSABLE_IVA");
        assertThat(editado.responsabilidadesFiscales()).containsExactly("O-13");
    }
}
