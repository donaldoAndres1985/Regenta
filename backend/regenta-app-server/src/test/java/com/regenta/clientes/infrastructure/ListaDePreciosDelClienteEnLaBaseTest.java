package com.regenta.clientes.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.jdbc.core.JdbcTemplate;

import com.regenta.PostgresDePrueba;
import com.regenta.clientes.domain.Cliente;
import com.regenta.clientes.domain.TipoDocumento;
import com.regenta.clientes.domain.TipoPersona;

/** HU-132 · La lista de precios del cliente va a la columna {@code crm.clientes.lista_precios_id} que ya existía. */
@DataJpaTest
class ListaDePreciosDelClienteEnLaBaseTest extends PostgresDePrueba {

    @Autowired
    private ClienteRepositorio clientes;
    @Autowired
    private TestEntityManager em;
    @Autowired
    private JdbcTemplate jdbc;

    @Test
    @DisplayName("Se guarda y se lee la lista de precios del cliente")
    void idaYVuelta() {
        UUID negocio = UUID.randomUUID();
        UUID lista = UUID.randomUUID();
        comoElNegocio(jdbc, negocio);
        Cliente c = Cliente.crear(negocio, UUID.randomUUID(), TipoPersona.JURIDICA, TipoDocumento.NIT, "900111222", "3",
                null, null, "Ferretería El Tornillo SAS", null, null, null, "MAYORISTA", null);
        c.usarListaDePrecios(lista);
        clientes.saveAndFlush(c);
        em.clear();

        assertThat(clientes.findByIdAndEliminadoEnIsNull(c.getId())).hasValueSatisfying(
                leido -> assertThat(leido.getListaPreciosId()).isEqualTo(lista));
    }
}
